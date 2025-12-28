package org.neatore.calisync;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

public class CaliSync {
    public static Logger LOGGER = LogManager.getLogger(CaliSync.class);
    public static void main(String[] args) throws URISyntaxException {
        LOGGER.info("Opening connection to CaliSync server...");
        Client client = new Client(new URI("ws://localhost:8080/calisync"));
        client.connect();
    }
}
