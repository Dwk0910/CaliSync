package org.neatore.caliback.services;

import static org.neatore.caliback.util.Response.response;

import jakarta.annotation.PreDestroy;

import org.neatore.caliback.CaliBack;
import org.neatore.caliback.abs.IdentableObject;
import org.neatore.caliback.abs.ServerEventSender;
import org.neatore.caliback.object.PacketResponse;
import org.neatore.caliback.object.SSEResponse;
import org.neatore.caliback.object.SSESession;
import org.neatore.caliback.util.EmitterSender;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class AutoUpdateService extends ServerEventSender {
    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    private final Set<SSESession> sse_sessions = new CopyOnWriteArraySet<>();

    @Override
    public void addSession(IdentableObject session) {
        if (session instanceof SSESession sse) sse_sessions.add(sse);
        else sessions.add((WebSocketSession) session);
    }

    @Override
    public void removeSession(IdentableObject session) {
        if (session instanceof SSESession sse) sse_sessions.removeIf(s -> s.getSessionId().equals(sse.getSessionId()));
        else sessions.remove((WebSocketSession) session);
    }

    @PreDestroy
    public void completeAllEvents() {
        sessions.forEach(session -> {
            try {
                session.close();
            } catch (IOException e) {
                CaliBack.LOGGER.error("Error while closing sessions", e);
            }
        });
        sse_sessions.forEach(session -> session.getSession().complete());
    }

    public void trigger(String senderId) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen() && !session.getId().equals(senderId)) response(session, new PacketResponse(600, null));
        }

        final Set<SSESession> unavailableSessions = new HashSet<>();
        for (SSESession session : sse_sessions) {
            if (!session.getSessionId().equals(senderId)) {
                boolean b = EmitterSender.send(session.getSession(), new SSEResponse(600, null));
                if (!b) unavailableSessions.add(session);
                Thread.yield();
            }
        }

        unavailableSessions.forEach(sse_sessions::remove);
    }
}
