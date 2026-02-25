package org.neatore.calisync.util;

import org.jetbrains.annotations.NotNull;

import org.json.JSONArray;
//import org.json.JSONObject;
//
//import org.neatore.calisync.CaliSync;
//import org.neatore.calisync.object.Date;
//
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//
//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;

public final class HistoryUtils {
    public static JSONArray getHistory(@NotNull String dburl, @NotNull String it_unique_id) {
//        try (Connection conn = DriverManager.getConnection(dburl);
//             PreparedStatement pstmt = conn.prepareStatement("SELECT it_history FROM item_table WHERE it_unique_id = ?;")
//        ) {
//            pstmt.setString(1, it_unique_id);
//            ResultSet rs = pstmt.executeQuery();
//            if (rs.next()) return HistoryUtils.decode(rs.getString("it_history"));
//        } catch (SQLException e) {
//            CaliSync.LOGGER.error("", e);
//        }
//        return null;

        return null;
    }

    public static JSONArray addHistory(@NotNull String dburl, @NotNull String it_unique_id, @NotNull String it_content) {
//        JSONArray array = Objects.requireNonNull(getHistory(dburl, it_unique_id));
//
//        JSONObject content = new JSONObject();
//        content.put("content", it_content);
//        content.put("time", Date.Now.getUnixTime());
//
//        array.put(content);
//        return array;

        return new JSONArray();
    }

    public static String addHistory(String appended, String appender_content) {
//        JSONArray array = HistoryUtils.decode(appended);
//        array.put(new JSONObject().put("content", appender_content).put("time", Date.Now.getUnixTime()));
//        return HistoryUtils.encode(array);

        return "";
    }

    public static String encode(@NotNull JSONArray history) {
//        // 순서 정렬 (content -> time)
//        List<Map<String, Object>> history_ = new ArrayList<>();
//        for (Object o : history) {
//            JSONObject obj = (JSONObject) o;
//            Map<String, Object> map = new LinkedHashMap<>();
//            map.put("content", obj.get("content"));
//            map.put("time", obj.getLong("time"));
//            history_.add(map);
//        }
//
//        String jsonString = new JSONArray(history_).toString()
//                // JSON 규격 준수
//                .replace("\n", "\\n")
//                .replace("\r", "\\r");
//
//        // JSON unicode escape
//        StringBuilder sb = new StringBuilder();
//        for (char c : jsonString.toCharArray()) {
//            if (c > 127) sb.append(String.format("\\u%04x", (int) c));
//            else sb.append(c);
//        }
//
//        return sb.toString().replace("\"", "|&quot;|");

        return "";
    }

    public static JSONArray decode(@NotNull String content) {
//        String str = content.replace("|&quot;|", "\"")
//                .replace("\n", "\\n")
//                .replace("\r", "\\r");
//
//        JSONArray array = new JSONArray(str);
//
//        List<Map<String, Object>> newArray = new ArrayList<>();
//        for (Object o : array) {
//            JSONObject obj = (JSONObject) o;
//            // content가 먼저, time이 뒤에 나와야 하므로 순서를 보장하는 LinkedHashMap 사용
//            Map<String, Object> newobj = new LinkedHashMap<>();
//            newobj.put("content", obj.getString("content"));
//            newobj.put("time", obj.getLong("time"));
//            newArray.add(newobj);
//        }
//
//        return new JSONArray(newArray);

        return new JSONArray();
    }
}
