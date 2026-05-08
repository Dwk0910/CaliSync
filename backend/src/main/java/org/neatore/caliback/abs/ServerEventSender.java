package org.neatore.caliback.abs;

public abstract class ServerEventSender {
    public abstract void addSession(IdentableObject session);
    public abstract void removeSession(IdentableObject session);
}
