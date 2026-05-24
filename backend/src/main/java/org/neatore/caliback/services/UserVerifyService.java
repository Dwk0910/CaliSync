package org.neatore.caliback.services;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.neatore.caliback.CaliBack.clientPassword;

@Service
public class UserVerifyService {
    private final Set<String> allowedSessions = new HashSet<>();

    public String addSession() {
        UUID uuid = UUID.randomUUID();
        allowedSessions.add(uuid.toString());
        return uuid.toString();
    }

    public void deleteSession(String token) {
        allowedSessions.remove(token);
    }

    public boolean verify(String token) {
        return allowedSessions.contains(token) || token.equals(clientPassword);
    }
}
