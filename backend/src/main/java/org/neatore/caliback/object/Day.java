package org.neatore.caliback.object;

import org.json.JSONObject;

import org.neatore.caliback.util.HistoryParser;

import java.util.List;

public record Day(Date date, List<Schedule> scheduleList, Date mdate, String bgColor, String history) {
    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        obj.put("date", date.getDate(1));

        var schedules = scheduleList.stream().map(Schedule::toJSONObject).toList();
        obj.put("schedules", schedules);

        obj.put("mdate", mdate.getDate(0));
        obj.put("bgColor", bgColor);
        obj.put("history", HistoryParser.decode(history));
        return obj;
    }
}
