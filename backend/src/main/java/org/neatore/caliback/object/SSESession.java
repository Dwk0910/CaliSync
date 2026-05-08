package org.neatore.caliback.object;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.neatore.caliback.abs.IdentableObject;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Getter
@AllArgsConstructor
public class SSESession extends IdentableObject {
    private String sessionId;
    private SseEmitter session;

    @Override public String getId() { return sessionId; }
}
