package org.neatore.caliback.services;

import org.json.JSONException;
import org.json.JSONObject;

import org.neatore.caliback.object.Date;

import org.neatore.caliback.object.PacketResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

import static org.neatore.caliback.CaliBack.dbc;

@Service
public class DBCService {
    private final PacketResponse e404 = new PacketResponse(404, "Not Found"),
            e400 = new PacketResponse(400, "Bad Request"),
            e500 = new PacketResponse(500, "Internal Server Error");

    public PacketResponse process(JSONObject obj) {
        try {
            switch (obj.getString("method")) {
                case "POST" -> {
                    try {
                        Map<String, Object> map = new JSONObject(
                                Objects.requireNonNull(obj.getString("data"))
                        ).toMap();
                        Date date = Date.parseDate(map.get("date").toString());
                        String content = map.get("content").toString();
                        dbc.addSchedule(date, content);
                        return new PacketResponse(201, null);
                    } catch (NullPointerException e) { return e400; }
                }

                case "DELETE" -> {
                    return e404;
                }

                case "GET" -> {
                    return e404;
                }

                default -> {
                    return e404;
                }
            }
        } catch (JSONException e) { return e400; }
          catch (Exception e) { return e500; }
    }
}
