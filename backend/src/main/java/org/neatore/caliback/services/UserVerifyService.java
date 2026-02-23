package org.neatore.caliback.services;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class UserVerifyService {
    private final Set<String> allowedSessions = new HashSet<>();

    public String addSession() {
        UUID uuid = UUID.randomUUID();
        allowedSessions.add(uuid.toString());
        return uuid.toString();
    }

    public void deleteSession(String uuid) {
        allowedSessions.remove(uuid);
    }

    public boolean verify(String uuid) {
        return allowedSessions.contains(uuid);
    }
}
