package org.neatore.caliback.util;

import org.jetbrains.annotations.NotNull;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

public abstract class HandlerAdapter implements WebSocketHandler {
    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {}

    @Override
    public void handleMessage(@NotNull WebSocketSession session, @NotNull WebSocketMessage<?> message) {}

    @Override
    public void handleTransportError(@NotNull WebSocketSession session, @NotNull Throwable exception) {}

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus closeStatus) {}

    @Override
    public boolean supportsPartialMessages() { return false; }
}
