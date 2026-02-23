package org.neatore.caliback;

import org.jetbrains.annotations.NotNull;

import org.neatore.caliback.controller.HardUpdateController;
import org.neatore.caliback.object.Date;
import org.neatore.caliback.object.Day;
import org.neatore.caliback.object.Schedule;
import org.neatore.caliback.util.DataUtils;
import org.neatore.caliback.util.HistoryUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DBC (String dburl, String u_mid) {
    public DBC(String dburl) {
        this(dburl, getUID(dburl));
    }

    private static String getUID(String dburl) {
        String sql = "SELECT u_mid FROM item_table";
        String u_mid = null;
        try (Connection conn = DriverManager.getConnection(dburl);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) u_mid = rs.getString("u_mid");
        } catch (SQLException e) {
            CaliBack.LOGGER.error("", e);
            System.exit(-1);
        }

        return u_mid;
    }

    public List<Schedule> getSchedules(Date date) {
        String it_unique_id = "dkcal_mdays_" + date.getDate(1);
        try (Connection conn = DriverManager.getConnection(dburl);
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM item_table WHERE it_unique_id = ?;")) {
            pstmt.setString(1, it_unique_id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return DataUtils.getSchedules(rs);
        } catch (SQLException e) {
            CaliBack.LOGGER.error("", e);
            throw new RuntimeException();
        }
        return null;
    }

    public JSONArray getSchedulesAsJson(Date date) {
        JSONArray result = new JSONArray();
        List<Schedule> schedules = getSchedules(date);
        if (schedules == null) return result;

        for (Schedule schedule : schedules) {
            JSONObject obj = new JSONObject();
            obj.put("content", schedule.content);
            obj.put("date", schedule.date.getDate(1));
            obj.put("isCompleted", schedule.isCompleted);
            result.put(obj);
        }

        return result;
    }

    public JSONArray getMonthSchedules(Date date) {
        List<Day> result;
        try (Connection conn = DriverManager.getConnection(dburl);
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM item_table WHERE it_unique_id LIKE ?;")
        ) {
            pstmt.setString(1, "dkcal_mdays_" + date.year + date.month + "%");
            ResultSet rs = pstmt.executeQuery();
            result = DataUtils.getDays(rs);
        } catch (SQLException | IllegalArgumentException e) {
            CaliBack.LOGGER.error("", e);
            throw new RuntimeException(e);
        }

        return new JSONArray(result.stream()
                .map(d -> {
                    JSONObject obj = new JSONObject();
                    obj.put("date", d.date().getDate(1));
                    obj.put("bgColor", d.bgColor());
                    obj.put("mdate", d.mdate().getDate(1));
                    JSONArray schedArray = new JSONArray();
                    int i = 0;
                    for (Schedule sched : d.schedules()) {
                        JSONObject schedObj = new JSONObject();
                        schedObj.put("content", sched.content);
                        schedObj.put("date", sched.date.getDate(1));
                        schedObj.put("isCompleted", sched.isCompleted);

                        // 나중에 드래그를 구현할 때 고유 id가 있어야 함
                        schedObj.put("id", "%s-%d".formatted(date, i));
                        schedArray.put(schedObj);
                        i++;
                    }
                    obj.put("schedules", schedArray);
                    return obj;
                })
                .toList()
        );
    }

    public JSONObject getUpdateInfo() {
        JSONObject obj = new JSONObject();
        try (Connection conn = DriverManager.getConnection(dburl);
             PreparedStatement pstmt_last_m = conn.prepareStatement("SELECT MAX(it_mdate) AS last_modified FROM item_table;");
             PreparedStatement pstmt_count = conn.prepareStatement("SELECT COUNT(*) AS total_count FROM item_table;")
        ) {
            ResultSet rs = pstmt_last_m.executeQuery();
            if (rs.next()) obj.put("last_modified", rs.getString("last_modified"));

            rs = pstmt_count.executeQuery();
            if (rs.next()) obj.put("total_count", Integer.toString(rs.getInt("total_count")));
        } catch (SQLException e) {
            CaliBack.LOGGER.error("", e);
            throw new RuntimeException(e);
        }

        return obj;
    }

    public String getDownloadKey() {
        String key = UUID.randomUUID().toString();
        HardUpdateController.VALID_KEYS.put(key, System.currentTimeMillis() + 60000); // 60 seconds validity
        return key;
    }

    public void update(JSONArray targets) {
        List<Day> days = new ArrayList<>();
        for (int i = 0; i < targets.length(); i++) {
            JSONObject obj = targets.getJSONObject(i);

            Date date = Date.parseDate(obj.getString("date"));
            List<Schedule> schedules = new ArrayList<>();
            JSONArray scheduleArray = obj.getJSONArray("schedules");
            for (int j = 0; j < scheduleArray.length(); j++) {
                JSONObject schedObj = scheduleArray.getJSONObject(j);
                schedules.add(new Schedule(date, schedObj.getString("content")));
            }
            days.add(new Day(
                    date,
                    schedules,
                    Date.parseDate(obj.getString("mdate")),
                    obj.getString("bgColor"),
                    HistoryUtils.encode(obj.getJSONArray("history"))
            ));
        }
        update(days);
    }

    public void update(List<Day> days) {
        // Mission : Insert given day with whole attributes into DB (or update if already exists)
        for (Day day : days) {
            JSONObject dayObj = day.toJSONObject();
            // 1차 업데이트 시도
            try (Connection conn = DriverManager.getConnection(dburl);
                 PreparedStatement pstmt = conn.prepareStatement("UPDATE item_table SET it_bgcolor = ?, it_content = ?, it_history = ?, it_mdate = ? WHERE it_unique_id = ?;")
            ) {
                StringBuilder content = new StringBuilder();
                day.schedules().forEach(i -> content.append(content.isEmpty() ? i.content : "\n" + i.content));
                pstmt.setString(1, dayObj.getString("bgColor"));
                pstmt.setString(2, content.toString());
                pstmt.setString(3, day.history());
                pstmt.setString(4, dayObj.getString("mdate"));
                pstmt.setString(5, day.date().getUniqueId());
                int affectedRows = pstmt.executeUpdate();

                // 업데이트된 행이 없으면(기존 데이터가 없으면) INSERT 시도
                if (affectedRows == 0) {
                    try (PreparedStatement insertPstmt = conn.prepareStatement("INSERT INTO item_table (u_mid, it_unique_id, it_bgcolor, it_content, it_history, it_cdate, it_mdate, it_mtime) VALUES (?, ?, ?, ?, ?, ?, ?, ?);")) {
                        insertPstmt.setString(1, u_mid);
                        insertPstmt.setString(2, day.date().getUniqueId());
                        insertPstmt.setString(3, dayObj.getString("bgColor"));
                        insertPstmt.setString(4, content.toString());
                        insertPstmt.setString(5, day.history());
                        insertPstmt.setString(6, dayObj.getString("mdate"));
                        insertPstmt.setString(7, dayObj.getString("mdate"));
                        insertPstmt.setInt(8, 1);

                        int insertAffectedRows = insertPstmt.executeUpdate();
                        if (insertAffectedRows > 0) CaliBack.LOGGER.info("Inserting data for {} has been succeed.", day.date().getDate(2));
                    }
                } else {
                    CaliBack.LOGGER.info("Updating data for {} has been succeed.", day.date().getDate(2));
                }
            } catch (SQLException e) {
                CaliBack.LOGGER.error("", e);
                throw new RuntimeException();
            }
        }
    }

    public void addSchedule(Schedule schedule) { addSchedule(schedule.date, schedule.content); }
    public void addSchedule(Schedule schedule, String it_date) { addSchedule(schedule.date, schedule.content, it_date); }
    public void addSchedule(Date date, String content) { addSchedule(date, content, Date.Now.format("yyyy-MM-dd HH:mm:ss")); }
    public void addSchedule(Date date, String content, String it_date) {
        String it_unique_id = date.getUniqueId();
        try (Connection conn = DriverManager.getConnection(dburl)) {
            boolean exists = false;
            String tHistory, tContent = null;

            Map<String, Object> data = DataUtils.getRecordData(it_unique_id);
            // Checks that data exists
            // 해당 날짜 데이터 존재
            if (!data.isEmpty()) {
                exists = true;

                // 업데이트를 위해 기존 데이터에 새로운 데이터 추가
                String orig_content_str = data.get("it_content").toString();
                if (!orig_content_str.isEmpty()) tContent = orig_content_str + "\n" + content;
                else tContent = content;

                // 기존 history에 새로운 history 추가
                JSONArray array = HistoryUtils.decode(data.get("it_history").toString());

                JSONObject obj = new JSONObject();
                obj.put("content", tContent);
                obj.put("time", Date.Now.getUnixTime());
                array.put(obj);

                tHistory = HistoryUtils.encode(array);
            } else {
                // 기존 데이터가 없으므로 새로운 history 생성
                JSONArray array = new JSONArray();
                JSONObject obj = new JSONObject();
                obj.put("content", content);
                obj.put("time", Date.Now.getUnixTime());
                array.put(obj);

                tHistory = HistoryUtils.encode(array);
            }

            // UPSERT
            int affectedRows;
            if (exists) {
                // 기존 데이터가 있으므로 UPDATE
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE item_table SET it_content = ?, it_history = ?, it_mdate = ? WHERE it_unique_id = ?;")) {
                    //                                                                               1                2             3                     4
                    pstmt.setString(1, tContent);
                    pstmt.setString(2, tHistory);
                    pstmt.setString(3, it_date);
                    pstmt.setString(4, it_unique_id);

                    affectedRows = pstmt.executeUpdate();
                }
            } else {
                // 기존 데이터가 없으므로 INSERT
                try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO item_table (u_mid, it_unique_id, it_content, it_history, it_cdate, it_mdate, it_mtime) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    //                                                                             1         2            3            4         5          6         7
                    pstmt.setString(1, u_mid);
                    pstmt.setString(2, it_unique_id);
                    pstmt.setString(3, content);
                    pstmt.setString(4, tHistory);
                    pstmt.setString(5, it_date);
                    pstmt.setString(6, it_date);
                    pstmt.setInt(7, 1);

                    affectedRows = pstmt.executeUpdate();
                }
            }

            if (affectedRows > 0) CaliBack.LOGGER.info("Appending data has been succeed.");
        } catch (SQLException e) {
            CaliBack.LOGGER.error("", e);
            throw new RuntimeException();
        }
    }

    public void setSchedule(Date date, String content) {
        Map<String, Object> data = DataUtils.getRecordData(date.getUniqueId());
        int affectedRows;

        try {
            if (!data.isEmpty()) {
                if (data.get("it_content").toString().equals(content)) {
                    CaliBack.LOGGER.warn("Setting data request has been rejected. Original content and target content should not be same.");
                    return;
                }

                // Prepare for pstmt inserting (history, date)
                String str_2 = HistoryUtils.addHistory(data.get("it_history").toString(), content);
                String str_3 = Date.Now.toDate().getDate(0);

                try (Connection conn = DriverManager.getConnection(dburl);
                     PreparedStatement pstmt = conn.prepareStatement("UPDATE item_table SET it_content = ?, it_history = ?, it_mdate = ? WHERE it_unique_id = ?")
                ) {
                    pstmt.setString(1, content);
                    pstmt.setString(2, str_2);
                    pstmt.setString(3, str_3);
                    pstmt.setString(4, date.getUniqueId());

                    affectedRows = pstmt.executeUpdate();
                } catch (SQLException e) {
                    CaliBack.LOGGER.error("", e);
                    throw new RuntimeException(e);
                }
            } else {
                addSchedule(date, content);
                affectedRows = 1;
            }

            if (affectedRows > 0) CaliBack.LOGGER.info("Setting data for {} has been succeed.", date.getDate(2));
        } catch (Exception e) {
            CaliBack.LOGGER.error("Error occured while running setSchedule() method: {}", e);
        }
    }

    public void removeSchedule(Date date, int seq) {
        String it_unique_id = date.getUniqueId();

        List<Schedule> schedules = getSchedules(date);
        if (schedules == null || schedules.isEmpty()) {
            CaliBack.LOGGER.error("No schedules found for {}", date.getDate(2));
            return;
        }

        StringBuilder it_content = new StringBuilder();

        int i = 1;
        boolean changed = false;
        for (Schedule schedule : schedules) {
            if (i != seq) it_content.append(i == 1 ? schedule.content : "\n" + schedule.content);
            else if (!schedule.content.isEmpty()) changed = true;
            i++;
        }

        if (changed) {
            String newHistory = HistoryUtils.encode(HistoryUtils.addHistory(dburl, it_unique_id, it_content.toString()));

            int affectedRows = 0;
            try (Connection conn = DriverManager.getConnection(dburl);
                 PreparedStatement pstmt = conn.prepareStatement("UPDATE item_table SET it_content = ?, it_history = ?, it_mdate = ? WHERE it_unique_id = ?;")) {
                //                                                                              1               2             3                    4
                pstmt.setString(1, it_content.toString());
                pstmt.setString(2, newHistory);
                pstmt.setString(3, Date.Now.format("yyyy-MM-dd HH:mm:ss"));
                pstmt.setString(4, it_unique_id);

                affectedRows = pstmt.executeUpdate();
            } catch (SQLException e) {
                CaliBack.LOGGER.error("", e);
            }

            if (affectedRows > 0) CaliBack.LOGGER.info("Removing data has been succeed.");
        } else {
            CaliBack.LOGGER.info("No content found for sequence {}", seq);
        }
    }

    public void removeAllSchedules(@NotNull Date date) {
        List<Schedule> schedules = getSchedules(date);
        if (schedules == null || schedules.isEmpty()) {
            CaliBack.LOGGER.info("No schedules found for {}", date.getDate(2));
            return;
        }

        String newHistory = HistoryUtils.encode(HistoryUtils.addHistory(dburl, date.getUniqueId(), ""));
        try (Connection conn = DriverManager.getConnection(dburl);
            PreparedStatement pstmt = conn.prepareStatement("UPDATE item_table SET it_content = ?, it_history = ?, it_mdate = ? WHERE it_unique_id = ?;")) {
            pstmt.setString(1, "");
            pstmt.setString(2, newHistory);
            pstmt.setString(3, Date.Now.format("yyyy-MM-dd HH:mm:ss"));
            pstmt.setString(4, date.getUniqueId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) CaliBack.LOGGER.info("Schedules on {} have been removed.", date.getDate(2));
        } catch (SQLException e) {
            CaliBack.LOGGER.error("", e);
            throw new RuntimeException();
        }
    }

    public void removeRecord(@NotNull Date date) {
        if (getSchedules(date) == null) {
            CaliBack.LOGGER.error("No record found for {}", date.getDate(2));
            return;
        }

        try (Connection conn = DriverManager.getConnection(dburl);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM item_table WHERE it_unique_id = ?;")) {
            pstmt.setString(1, date.getUniqueId());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) CaliBack.LOGGER.info("Records on {} have been removed.", date.getDate(2));
        } catch (SQLException e) {
            CaliBack.LOGGER.error("", e);
            throw new RuntimeException();
        }
    }
}
