package org.neatore.calisync.util;

import org.jetbrains.annotations.NotNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HistoryParser {
    public static String encode(@NotNull JSONArray history) {
        // 순서 정렬 (content -> time)
        List<Map<String, Object>> history_ = new ArrayList<>();
        for (Object o : history) {
            JSONObject obj = (JSONObject) o;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("content", obj.get("content"));
            map.put("time", obj.get("time"));
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