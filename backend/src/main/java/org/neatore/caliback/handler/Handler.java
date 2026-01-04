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
        response(session, response.responseCode(), response.responseBody());
    }

    private static <T> void response(WebSocketSession session, int response_code, @Nullable T body) {
        try {
            JSONObject response = new JSONObject();
            response.put("code", response_code);
            response.put("body", body);
            session.sendMessage(new TextMessage(response.toString()));
        } catch (IOException e) {
            CaliBack.LOGGER.error(e);
        }
    }
}
