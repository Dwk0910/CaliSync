package org.neatore.caliback.services;

import static org.neatore.caliback.util.Response.response;

import org.neatore.caliback.CaliBack;
import org.neatore.caliback.object.PacketResponse;
import org.neatore.caliback.object.SSEResponse;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class AutoUpdateService {
    public record SSESession(String sessionId, SseEmitter emitter) {
    }

    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    private final Set<SSESession> sse_sessions = Collections.synchronizedSet(new HashSet<>());

    public void addSession(SSESession session) {
        sse_sessions.add(session);
    }
    public void addSession(WebSocketSession session) {
        sessions.add(session);
    }

    public void removeSession(SseEmitter session) { sse_sessions.removeIf(s -> s.emitter.equals(session)); }
    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }

    public void trigger(String senderId) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen() && !session.getId().equals(senderId)) response(session, new PacketResponse(600, null));
        }

        try {
            for (SSESession session : sse_sessions) {
                if (!session.sessionId.equals(senderId))
                    session.emitter.send(SseEmitter.event().data(new SSEResponse(600, null)).build());
            }
        } catch (IOException e) {
            CaliBack.LOGGER.error("", e);
        }
    }
}
