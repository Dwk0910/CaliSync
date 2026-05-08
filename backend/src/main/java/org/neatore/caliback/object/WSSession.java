package org.neatore.caliback.object;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.neatore.caliback.abs.IdentableObject;

import org.springframework.web.socket.WebSocketSession;

@Getter
@AllArgsConstructor
public class WSSession extends IdentableObject {
    private String sessionId;
    private WebSocketSession session;

    @Override public String getId() { return sessionId; }
}
