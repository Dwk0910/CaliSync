package org.neatore.calisync;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.neatore.calisync.service.DBWatcher;

import java.net.URI;
import java.net.URISyntaxException;

import java.nio.file.Path;

import java.util.concurrent.TimeUnit;

public class CaliSync {
    public static final Path dbDir = Path.of(System.getProperty("user.home"), "AppData", "Roaming", "CalendarTask", "Db");
    public static final Path dbPath = Path.of(dbDir.toString(), "calendar.db");

    public static Logger LOGGER = LogManager.getLogger(CaliSync.class);
    public static void main(String[] args) throws URISyntaxException {
        if (!dbPath.toFile().exists()) {
            LOGGER.fatal("Could not find CalendarTask database at {}", dbPath.toString());
            return;
        }

        LOGGER.info("Opening connection to the CaliSync server...");
        Client client = new Client(new URI("ws://localhost:8080/calisync"));
        try {
            client.connectBlocking(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            CaliSync.LOGGER.fatal(e);
        }

        // Local DB Watch Service 생성
        LOGGER.info("Registering CaliSync Event Listener...");
        new Thread(new DBWatcher(client)).start();
    }
}
