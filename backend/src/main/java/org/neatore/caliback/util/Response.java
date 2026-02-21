package org.neatore.caliback.util;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import org.json.JSONObject;

import org.neatore.caliback.CaliBack;
import org.neatore.caliback.object.PacketResponse;

import java.io.IOException;

public class Response {
    public static void response(WebSocketSession session, PacketResponse data) {
        try {
            JSONObject response = new JSONObject();
            response.put("code", data.getResponseCode());
            response.put("body", data.getResponseBody());
            response.put("requestId", data.getRequestId());
            session.sendMessage(new TextMessage(response.toString(), true));
        } catch (IOException e) {
            CaliBack.LOGGER.error(e);
        }
    }
}
