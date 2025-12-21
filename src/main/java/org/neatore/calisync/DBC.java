package org.neatore.calisync;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record DBC (String dburl) {
    public void addSchedule(String content) {
        long unixTime = System.currentTimeMillis() / 1000L;
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonContent = new JSONObject();
        jsonContent.put("content", content);
        jsonContent.put("time", unixTime);
        jsonArray.put(jsonContent);

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String sql = "INSERT INTO item_table (u_id, it_content, it_cdate, it_mdate) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dburl)) {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, 0);
            pstmt.setString(2, jsonArray.toString());
            pstmt.setString(3, now);
            pstmt.setString(4, now);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Data appending has been succeed.");
            }
        } catch (SQLException e) {
            CaliSync.LOGGER.error(e);
        }
    }
}
