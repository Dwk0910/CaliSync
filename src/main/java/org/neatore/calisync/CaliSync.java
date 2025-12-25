package org.neatore.calisync;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Date;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class CaliSync {
    public static Logger LOGGER = LogManager.getLogger(CaliSync.class);
    public static Path database = Paths.get(System.getenv("APPDATA"), "CalendarTask", "Db", "calendar.db");

    public static DBC dbc;

    public static void main(String[] args) {
        if (!database.toFile().exists()) {
            LOGGER.error("Database does not exist. Shutting down...");
            System.exit(-1);
        } else LOGGER.info("Database found. Initializing...");

        dbc = new DBC("jdbc:sqlite:" + database);
        LOGGER.info("Done.");

        // Test
        Date now = new Date();
        now.setTime(1767225600000L);
        dbc.addSchedule("반갑습니다", now.getTime());
    }
}
