package org.neatore.caliback;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CaliBack {
    public static Logger LOGGER = LogManager.getLogger(CaliBack.class);
    public static Path datapath = Paths.get(System.getProperty("user.dir"), "data");
    public static Path configurations = Paths.get(datapath.toString(), "config.json");

    public static Path database;
    public static DBC dbc;

    public static String allowedEmail;

    public static void main(String[] args) {
        // Parse configuration

        JSONObject configurationObj;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configurations.toFile()), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[16384];
            int bytesIn;
            while ((bytesIn = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, bytesIn);
            }

            configurationObj = new JSONObject(sb.toString());
            database = Paths.get(configurationObj.getJSONObject("CaliBack").getString("database_path"));
            allowedEmail = configurationObj.getJSONObject("CaliBack").getString("allowed_email");
        } catch (IOException | JSONException e) {
            LOGGER.fatal("Error while loading configuration file. Please check configuration file exists, or is valid.");
            LOGGER.fatal("Configuration file has to be named \"config.json\" and located at {}", Paths.get(System.getProperty("user.dir"), "data").toString());
            System.exit(-1);
        }

        if (!database.toFile().exists()) {
            LOGGER.error("Database does not exist. Shutting down...");
            System.exit(-1);
        } else LOGGER.info("Initializing...");

        dbc = new DBC("jdbc:sqlite:" + database);
        LOGGER.info("Initialization complete. Starting Springboot application...");
        SpringApplication.run(CaliBack.class, args);
    }
}
