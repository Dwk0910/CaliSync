package org.neatore.caliback.services;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.neatore.caliback.CaliBack;
import org.neatore.caliback.object.Date;
import org.neatore.caliback.object.PacketResponse;

import org.springframework.stereotype.Service;

import static org.neatore.caliback.CaliBack.dbc;

@Service
public class DBCService {
    private final PacketResponse e404 = new PacketResponse(404, "Not Found"),
            e400 = new PacketResponse(400, "Bad Request"),
            e500 = new PacketResponse(500, "Internal Server Error");

    public PacketResponse process(JSONObject obj) {
        try {
            JSONObject data = obj.getJSONObject("data");
            Date date = data.isNull("date") ? new Date("", "", "") : Date.parseDate(data.get("date").toString());

            switch (obj.getString("method")) {
                case "POST" -> {
                    try {
                        String content = data.get("content").toString();
                        dbc.addSchedule(date, content);
                        return new PacketResponse(201, null);
                    } catch (NullPointerException e) { return e400; }
                }

                case "DELETE" -> {
                    try {
                        int seq = Integer.parseInt(data.get("seq").toString());
                        dbc.removeSchedule(date, seq);
                        return new PacketResponse(204, null);
                    } catch (NullPointerException | NumberFormatException e) { throw new JSONException(""); }
                }

                case "GET" -> {
                    return new PacketResponse(200, dbc.getSchedulesAsJson(date));
                }

                case "UPDATE_INFO" -> {
                    return new PacketResponse(200, dbc.getUpdateInfo());
                }

                case "UPDATE" -> {
                    JSONArray array = new JSONArray(data.getJSONArray("targets"));
                    dbc.update(array);
                    return new PacketResponse(200, null);
                }

                default -> { return e404; }
            }
        } catch (IllegalArgumentException | JSONException e) { return e400; }
          catch (Exception e) {
            CaliBack.LOGGER.error(e);
            return e500;
        }
    }
}
