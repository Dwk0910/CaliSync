package org.neatore.caliback.handler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.json.JSONObject;

import org.neatore.caliback.CaliBack;
import org.neatore.caliback.object.PacketResponse;
import org.neatore.caliback.services.DBCService;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

public class Handler extends TextWebSocketHandler {
    private final DBCService dbcService;

    public Handler(DBCService dbcService) {
        this.dbcService = dbcService;
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) {
        JSONObject obj = new JSONObject(message.getPayload());
        PacketResponse response = dbcService.process(obj);
        response(session, obj, response.responseCode(), response.responseBody());
    }

    private static <T> void response(WebSocketSession session, JSONObject message, int response_code, @Nullable T body) {
        JSONObject data = message.getJSONObject("data");

        try {
            JSONObject response = new JSONObject();
            response.put("code", response_code);
            response.put("body", body);
            response.put("requestId", data.isNull("requestId") ? null : data.getString("requestId"));
            session.sendMessage(new TextMessage(response.toString()));
        } catch (IOException e) {
            CaliBack.LOGGER.error(e);
        }
    }
}
