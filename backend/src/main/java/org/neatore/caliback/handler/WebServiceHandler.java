package org.neatore.caliback.handler;

import org.jetbrains.annotations.NotNull;

import org.neatore.caliback.util.Response;
import org.neatore.caliback.object.PacketResponse;
import org.neatore.caliback.services.AutoUpdateService;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class WebServiceHandler extends TextWebSocketHandler {
    private final AutoUpdateService autoUpdateService;

    public WebServiceHandler(AutoUpdateService autoUpdateService) {
        this.autoUpdateService = autoUpdateService;
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {
        // sessionId 제공
        Response.response(session, new PacketResponse(0, session.getId()));

        autoUpdateService.addSession(session);
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        autoUpdateService.removeSession(session);
    }
}
