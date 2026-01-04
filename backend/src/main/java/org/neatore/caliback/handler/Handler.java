package org.neatore.caliback.handler;

import org.jetbrains.annotations.NotNull;

import org.json.JSONObject;
import org.neatore.caliback.CaliBack;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class Handler extends TextWebSocketHandler {
    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) {
        JSONObject obj = new JSONObject(message.getPayload());
        CaliBack.LOGGER.info("{}는 {}입니다.", obj.getString("name"), obj.getString("details"));
    }
}
