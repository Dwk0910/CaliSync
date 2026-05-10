package org.neatore.caliback.services;

import lombok.Getter;

import org.json.JSONArray;

import org.neatore.caliback.CaliBack;
import org.neatore.caliback.abs.IdentableObject;
import org.neatore.caliback.abs.ServerEventSender;
import org.neatore.caliback.object.SSEResponse;
import org.neatore.caliback.object.SSESession;
import org.neatore.caliback.util.EmitterSender;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class LogSenderService extends ServerEventSender {
    @Getter
    private final Set<SSESession> sessions = new CopyOnWriteArraySet<>();
    private JSONArray previous = new JSONArray();

    // singleton
    @Getter
    private static LogSenderService instance;
    public LogSenderService() {
        instance = this;
    }

    @Override
    public void addSession(IdentableObject session) {
        if (session instanceof SSESession sseSession) {
            sessions.add(sseSession);
            trigger();
        } else CaliBack.LOGGER.warn("LogSenderService: Tried to add non-SSE session (sessionId: {})", session.getId());
    }

    @Override
    public void removeSession(IdentableObject session) {
        if (session instanceof SSESession sseSession) sessions.removeIf(s -> s.getSessionId().equals(sseSession.getSessionId()));
    }

    public void logAll(JSONArray messageArr) {
        this.previous = new JSONArray(messageArr);
        trigger();
    }

    public void log(String message) {
        this.previous.put(message);
        trigger();
    }

    private void trigger() {
        for (SSESession session : sessions) {
            try {
                EmitterSender.send(session.getSession(), new SSEResponse(200, previous.toString()));
            } catch (IllegalStateException ignored) {
                this.removeSession(session);
            }
        }
    }
}
