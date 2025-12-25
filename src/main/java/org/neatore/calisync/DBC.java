package org.neatore.calisync;

import org.neatore.calisync.object.Date;
import org.neatore.calisync.object.Schedule;
import org.neatore.calisync.util.HistoryParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public record DBC (String dburl, String u_mid) {
    public DBC(String dburl) {
        this(dburl, getUID(dburl));
    }

    private static String getUID(String dburl) {
        String sql = "SELECT u_mid FROM \"item_table\"";
        String u_mid = null;
        try (Connection conn = DriverManager.getConnection(dburl);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) u_mid = rs.getString("u_mid");
        } catch (SQLException e) {
            CaliSync.LOGGER.error(e);
        }

        return u_mid;
    }

    public List<Schedule> getSchedules(Date date) {
        // 시분초는 사용하지 않으므로 리셋
        date.setTime("0", "0", "0");

        String it_unique_id = "dkcal_mdays_" + date.getDate(1);
        try (Connection conn = DriverManager.getConnection(dburl);
            PreparedStatement pstmt = conn.prepareStatement("SELECT it_content FROM item_table WHERE it_unique_id = ?;")) {
            pstmt.setString(1, it_unique_id);

            String result;
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) result = rs.getString("it_content");
            else return null;

            Scanner scan = new Scanner(result);
            List<Schedule> resultList = new ArrayList<>();
            while (scan.hasNext()) {
                // TODO: 취소선으로 isCompleted 판단하기
                resultList.add(new Schedule(date, scan.nextLine(), false));
            }
            return resultList;
        } catch (SQLException e) {
            CaliSync.LOGGER.error(e);
            return null;
        }
    }

    public void addSchedule(String content, long date) {
        long now = System.currentTimeMillis();

        DateTimeFormatter unique_id_formatter = DateTimeFormatter.ofPattern("yyyyMMdd"),
                it_date_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String it_unique_id = "dkcal_mdays_" + Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).format(unique_id_formatter),
                it_date = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).format(it_date_formatter);
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
                    obj.put("time", now);
                    array.put(obj);

                    tHistory = HistoryParser.encode(array);
                } else {
                    // 기존 데이터가 없으므로 새로운 history 생성
                    JSONArray array = new JSONArray();
                    JSONObject obj = new JSONObject();
                    obj.put("content", content);
                    obj.put("time", now);
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

            if (affectedRows > 0) CaliSync.LOGGER.info("Appending data has been succeed.");
        } catch (SQLException e) {
            CaliSync.LOGGER.error(e);
        }
    }
}
