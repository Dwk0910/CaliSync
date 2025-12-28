package org.neatore.caliback.handler;

import org.jetbrains.annotations.NotNull;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class Handler extends TextWebSocketHandler {
    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) {
        System.out.println("Received message: " + message.getPayload());
    }
}
