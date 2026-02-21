package org.neatore.calisync;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import org.neatore.calisync.packet.SignalPacket;
import org.neatore.calisync.service.DBWatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import java.net.URI;

import org.json.JSONObject;
import org.json.JSONException;

public class Client extends WebSocketClient {
    public Client(URI serverUri) { super(serverUri); }

    public void sendSignal(SignalPacket obj) {
        try {
            if (!this.isOpen()) {
                CaliSync.LOGGER.warn("Socket closed. Trying to reconnect...");
                this.reconnectBlocking();
            }

            this.send(obj.toString());
        } catch (InterruptedException e) {
            CaliSync.LOGGER.fatal("Failed to reconnect to the server.", e);
        }
    }

    private final Map<String, CompletableFuture<JSONObject>> pendingResponses = new HashMap<>();
    public CompletableFuture<JSONObject> sendSignalWithResponse(SignalPacket obj_) {
        return this.sendSignalWithResponse(obj_, 30);
    }

    public CompletableFuture<JSONObject> sendSignalWithResponse(SignalPacket obj_, int timeout) {
        String requestId = UUID.randomUUID().toString();
        JSONObject obj = obj_.toJSONObject();
        JSONObject data = obj.getJSONObject("data");
        data.put("requestId", requestId);
        obj.put("data", data);

        CompletableFuture<JSONObject> future = new CompletableFuture<>();
        pendingResponses.put(requestId, future);

        this.sendSignal(new SignalPacket(SignalPacket.Method.valueOf(obj.getString("method")), obj.getJSONObject("data").toMap()));
        return future.orTimeout(timeout, TimeUnit.SECONDS);
    }

    @Override
    public void onMessage(String s) {
        JSONObject obj = new JSONObject(s);
        try {
            if (obj.getInt("code") == 600) {
                new Thread(() -> {
                    CaliSync.LOGGER.info("The server sent refresh signal. Synchronizing with server database...");
                    DBWatcher.getInstance().synchronize();
                }).start();
                return;
            }

            String requestId = obj.getString("requestId");
            CompletableFuture<JSONObject> future = pendingResponses.get(requestId);
            if (future != null) {
                future.complete(obj);
                pendingResponses.remove(requestId);
            }
        } catch (JSONException e) {
            CaliSync.LOGGER.warn("The server responsed. but no requestId found. response: \n{}", obj.toString(4));
        }
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) { CaliSync.LOGGER.info("Connection established."); }

    @Override
    public void onClose(int i, String s, boolean b) { CaliSync.LOGGER.info("Connection closed."); }

    @Override
    public void onError(Exception e) {
        CaliSync.LOGGER.error(e);
        System.exit(-1);
    }
}
