package org.neatore.calisync.util;

import org.neatore.calisync.CaliSync;
import org.neatore.calisync.object.Date;

import org.jetbrains.annotations.NotNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HistoryParser {
    public static JSONArray getHistory(@NotNull String dburl, @NotNull String it_unique_id) {
        try (Connection conn = DriverManager.getConnection(dburl);
             PreparedStatement pstmt = conn.prepareStatement("SELECT it_history FROM item_table WHERE it_unique_id = ?;")
        ) {
            pstmt.setString(1, it_unique_id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return HistoryParser.decode(rs.getString("it_history"));
        } catch (SQLException e) {
            CaliSync.LOGGER.error(e);
        }
        return null;
    }

    public static JSONArray addHistory(@NotNull String dburl, @NotNull String it_unique_id, @NotNull String it_content) {
        JSONArray array = Objects.requireNonNull(getHistory(dburl, it_unique_id));

        JSONObject content = new JSONObject();
        content.put("content", it_content);
        content.put("time", Date.Now.getUnixTime());

        array.put(content);
        return array;
    }

    public static String encode(@NotNull JSONArray history) {
        // 순서 정렬 (content -> time)
        List<Map<String, Object>> history_ = new ArrayList<>();
        for (Object o : history) {
            JSONObject obj = (JSONObject) o;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("content", obj.get("content"));
            map.put("time", obj.getLong("time"));
            history_.add(map);
        }

        String jsonString = new JSONArray(history_).toString();

        // JSON unicode escape
        StringBuilder sb = new StringBuilder();
        for (char c : jsonString.toCharArray()) {
            if (c > 127) sb.append(String.format("\\u%04x", (int) c));
            else sb.append(c);
        }

        return sb.toString().replace("\"", "|&quot;|");
    }

    public static JSONArray decode(@NotNull String content) {
        JSONArray array = new JSONArray(content.replace("|&quot;|", "\""));

        List<Map<String, Object>> newArray = new ArrayList<>();
        for (Object o : array) {
            JSONObject obj = (JSONObject) o;
            // content가 먼저, time이 뒤에 나와야 하므로 순서를 보장하는 LinkedHashMap 사용
            Map<String, Object> newobj = new LinkedHashMap<>();
            newobj.put("content", obj.getString("content"));
            newobj.put("time", obj.getLong("time"));
            newArray.add(newobj);
        }

        return new JSONArray(newArray);
    }
}