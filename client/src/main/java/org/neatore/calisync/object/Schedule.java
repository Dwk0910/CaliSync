package org.neatore.calisync.object;

import org.json.JSONObject;

public class Schedule {
    public final Date date;
    public final String content;
    public final boolean isCompleted;

    public Schedule(Date date, String content) {
        this.date = date;
        this.content = content;
        // TODO : 완료 여부 content 통해서 판단하기
        this.isCompleted = false;
    }

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        obj.put("date", date.getDate(1));
        obj.put("content", content);
        obj.put("isCompleted", isCompleted);
        return obj;
    }
}
