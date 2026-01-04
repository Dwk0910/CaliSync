package org.neatore.calisync;

import org.neatore.calisync.packet.SignalPacket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class Client extends WebSocketClient {
    public Client(URI serverUri) { super(serverUri); }

    public void sendSignal(SignalPacket obj) {
        if (this.isOpen()) {
            this.send(obj.toString());
            CaliSync.LOGGER.info("Signal sent.");
        } else throw new RuntimeException("Socket is closed.");
    }

    @Override
    public void onMessage(String s) {
        System.out.println(s);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        CaliSync.LOGGER.info("Connection established.");
    }

    @Override
    public void onClose(int i, String s, boolean b) { CaliSync.LOGGER.info("Connection closed."); }

    @Override
    public void onError(Exception e) {
        CaliSync.LOGGER.error(e);
        System.exit(-1);
    }
}
