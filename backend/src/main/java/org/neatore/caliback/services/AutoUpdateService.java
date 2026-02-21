package org.neatore.caliback.services;

import static org.neatore.caliback.util.Response.response;

import org.neatore.caliback.object.PacketResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class AutoUpdateService {
    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());

    public void addSession(WebSocketSession session) {
        sessions.add(session);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }

    public void trigger(String senderId) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen() && !session.getId().equals(senderId)) response(session, new PacketResponse(600, null));
        }
    }
}
