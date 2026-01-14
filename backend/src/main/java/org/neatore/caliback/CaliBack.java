package org.neatore.caliback;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CaliBack {
    public static Logger LOGGER = LogManager.getLogger(CaliBack.class);
    public static Path database = Paths.get(System.getProperty("user.home"), "Documents", "Personal", "CaliSync", "test", "calendar.db");

    public static DBC dbc;

    public static void main(String[] args) {
        if (!database.toFile().exists()) {
            LOGGER.error("Database does not exist. Shutting down...");
            System.exit(-1);
        } else LOGGER.info("Initializing...");

        dbc = new DBC("jdbc:sqlite:" + database);
        LOGGER.info("Initialization complete. Starting Springboot application...");
        SpringApplication.run(CaliBack.class, args);
    }
}
