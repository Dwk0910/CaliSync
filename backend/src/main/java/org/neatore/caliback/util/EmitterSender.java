package org.neatore.caliback.util;

import org.neatore.caliback.object.SSEResponse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

public class EmitterSender {
    public static boolean send(SseEmitter emitter, SSEResponse data) throws IllegalStateException {
        try {
            emitter.send(SseEmitter.event().data(data).build());
        } catch (IOException e) {
            emitter.complete();
            return false;
        }

        return true;
    }
}
