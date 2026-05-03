package org.neatore.caliback.util;

import org.neatore.caliback.CaliBack;
import org.neatore.caliback.object.SSEResponse;

import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

public class EmitterSender {
    public static void send(SseEmitter emitter, SSEResponse data) {
        try {
            emitter.send(SseEmitter.event().data(data).build());
        } catch (IOException e) {
            if (e instanceof AsyncRequestNotUsableException) emitter.complete();
            else CaliBack.LOGGER.error("Error sending data", e);
        }
    }
}
