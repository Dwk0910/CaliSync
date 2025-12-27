package org.neatore.caliback;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class CaliBack {
    public static Logger LOGGER = LogManager.getLogger(CaliBack.class);
//    public static Path database = Paths.get(System.getenv("APPDATA"), "CalendarTask", "Db", "calendar.db");
    public static Path database = Paths.get(System.getProperty("user.dir"), "..", "test", "calendar.db");

    public static DBC dbc;

    public static void main(String[] args) {
        if (!database.toFile().exists()) {
            LOGGER.error("Database does not exist. Shutting down...");
            System.exit(-1);
        } else LOGGER.info("Database found. Initializing...");

        dbc = new DBC("jdbc:sqlite:" + database);
        LOGGER.info("Done.");
    }
}
