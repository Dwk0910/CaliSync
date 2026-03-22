package org.neatore.caliback.services;

import org.neatore.caliback.CaliBack;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

@Service
public class SSEClientService {
    private final Map<String, SseEmitter> clients = new HashMap<>();

    public SseEmitter getClient(String clientId) {
        SseEmitter emitter = clients.get(clientId);
        if (emitter == null) throw new IllegalArgumentException("No such client with id: " + clientId);

        return emitter;
    }

    public void addClient(String clientId, SseEmitter emitter) {
        clients.put(clientId, emitter);
    }

    public void removeClient(String clientId) {
        clients.remove(clientId);
    }

    public void sendEvent(String clientId, String name, Object data) {
        try {
            SseEmitter emitter = getClient(clientId);
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            CaliBack.LOGGER.error("", e);
        }
    }
}
