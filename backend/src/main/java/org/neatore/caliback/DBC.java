package org.neatore.caliback;

import org.jetbrains.annotations.NotNull;

import org.neatore.caliback.object.Date;
import org.neatore.caliback.object.Schedule;
import org.neatore.caliback.util.Analyze;
import org.neatore.caliback.util.HistoryParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;

public record DBC (String dburl, String u_mid) {
    public DBC(String dburl) {
        this(dburl, getUID(dburl));
    }

    private static String getUID(String dburl) {
        String sql = "SELECT u_mid FROM \"item_table\"";
        String u_mid = null;
        try (Connection conn = DriverManager.getConnection(dburl);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) u_mid = rs.getString("u_mid");
        } catch (SQLException e) {
            CaliBack.LOGGER.error(e);
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
            if (rs.next()) return Analyze.getSchedules(rs);
        } catch (SQLException e) {
            CaliBack.LOGGER.error(e);
        }
        return null;
    }

    public void addSchedule(Date date, String content) {
        String it_unique_id = "dkcal_mdays_" + date.getDate(1),
                it_date = Date.Now.format("yyyy-MM-dd HH:mm:ss");
        try (Connection conn = DriverManager.getConnection(dburl)) {
            boolean exists = false;
            String tHistory, tContent = null;

            // Checks that data exists
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM item_table WHERE it_unique_id = ?;")) {
                pstmt.setString(1, it_unique_id);
                ResultSet rs = pstmt.executeQuery();

                // 해당 날짜 데이터 존재
                if (rs.next()) {
                    exists = true;

                    // 업데이트를 위해 기존 데이터에 새로운 데이터 추가
                    String orig_content_str = rs.getString("it_content");
                    if (!orig_content_str.isEmpty()) tContent = orig_content_str + "\n" + content;
                    else tContent = content;

                    // 기존 history에 새로운 history 추가
                    JSONArray array = HistoryParser.decode(rs.getString("it_history"));

                    JSONObject obj = new JSONObject();
                    obj.put("content", tContent);
                    obj.put("time", Date.Now.getUnixTime());
                    array.put(obj);

                    tHistory = HistoryParser.encode(array);
                } else {
                    // 기존 데이터가 없으므로 새로운 history 생성
                    JSONArray array = new JSONArray();
                    JSONObject obj = new JSONObject();
                    obj.put("content", content);
                    obj.put("time", Date.Now.getUnixTime());
                    array.put(obj);

                    tHistory = HistoryParser.encode(array);
                }
            }

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
            CaliBack.LOGGER.error(e);
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
            if (i != seq) it_content.append(i == 1 ? schedule.content() : "\n" + schedule.content());
            else if (!schedule.content().isEmpty()) changed = true;
            i++;
        }

        if (changed) {
            String newHistory = HistoryParser.encode(HistoryParser.addHistory(dburl, it_unique_id, it_content.toString()));

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
                CaliBack.LOGGER.error(e);
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

        String newHistory = HistoryParser.encode(HistoryParser.addHistory(dburl, date.getUniqueId(), ""));
        try (Connection conn = DriverManager.getConnection(dburl);
            PreparedStatement pstmt = conn.prepareStatement("UPDATE item_table SET it_content = ?, it_history = ?, it_mdate = ? WHERE it_unique_id = ?;")) {
            pstmt.setString(1, "");
            pstmt.setString(2, newHistory);
            pstmt.setString(3, Date.Now.format("yyyy-MM-dd HH:mm:ss"));
            pstmt.setString(4, date.getUniqueId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) CaliBack.LOGGER.info("Schedules on {} have been removed.", date.getDate(2));
        } catch (SQLException e) {
            CaliBack.LOGGER.error(e);
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
            CaliBack.LOGGER.error(e);
        }
    }
}
