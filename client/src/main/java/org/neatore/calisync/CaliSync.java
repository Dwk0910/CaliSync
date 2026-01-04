package org.neatore.calisync;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.concurrent.TimeUnit;

public class CaliSync {
    public static Logger LOGGER = LogManager.getLogger(CaliSync.class);
    public static void main(String[] args) throws URISyntaxException {
        LOGGER.info("Opening connection to CaliSync server...");
        Client client = new Client(new URI("ws://localhost:8080/calisync"));
        try {
            client.connectBlocking(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            CaliSync.LOGGER.error(e);
        }

        JSONObject obj = new JSONObject();
        obj.put("name", "박종윤");
        obj.put("details", "qㅕㅇ신");

        client.sendSignal(obj);
    }
}
