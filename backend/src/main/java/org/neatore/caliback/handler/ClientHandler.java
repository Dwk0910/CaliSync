package org.neatore.caliback.handler;

import static org.neatore.caliback.util.Response.response;

import org.jetbrains.annotations.NotNull;

import org.json.JSONObject;

import org.neatore.caliback.object.PacketResponse;
import org.neatore.caliback.services.AutoUpdateService;
import org.neatore.caliback.services.DBCService;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class ClientHandler extends TextWebSocketHandler {
    private final AutoUpdateService autoUpdateService;
    private final DBCService dbcService;

    public ClientHandler(AutoUpdateService autoUpdateService, DBCService dbcService) {
        this.autoUpdateService = autoUpdateService;
        this.dbcService = dbcService;
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {
        autoUpdateService.addSession(session);
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        autoUpdateService.removeSession(session);
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) {
        JSONObject obj = new JSONObject(message.getPayload());
        PacketResponse response = dbcService.process(obj);

        try {
            // response가 필요없는 request의 경우 data 필드가 없을 수도 있음
            response.setRequestId(obj.getJSONObject("data").getString("requestId"));
        } catch (Exception ignored) {
        } finally {
            // 리로딩을 요구하는 요청일 경우 전체 리로드 트리거
            if (DBCService.doesRequireReload(obj.getString("method"))) autoUpdateService.trigger(session.getId());

            response(session, response);
        }
    }
}
