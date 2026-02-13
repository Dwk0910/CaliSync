package org.neatore.caliback.object;

import org.json.JSONObject;

import org.neatore.caliback.util.HistoryUtils;

import java.util.List;

public record Day(Date date, List<Schedule> schedules, Date mdate, String bgColor, String history) {
    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        obj.put("date", date.getDate(1));

        var _schedules = schedules.stream().map(Schedule::toJSONObject).toList();
        obj.put("schedules", _schedules);

        obj.put("mdate", mdate.getDate(0));
        obj.put("bgColor", bgColor);
        obj.put("history", HistoryUtils.decode(history));
        return obj;
    }
}
