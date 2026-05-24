package org.neatore.caliback;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONException;
import org.json.JSONObject;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CaliBack {
    public static Logger LOGGER = LoggerFactory.getLogger(CaliBack.class);
    public static Path datapath = Paths.get(System.getProperty("user.dir"), "data");
    public static Path configurations = Paths.get(datapath.toString(), "config.json");

    public static Path database;
    public static DBC dbc;

    public static String allowedEmail;
    public static String clientPassword;

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

            configurationObj = new JSONObject(sb.toString()).getJSONObject("CaliBack");

            database = Paths.get(configurationObj.getString("database_path"));
            allowedEmail = configurationObj.getString("allowed_email");
            clientPassword = configurationObj.getString("client_password");
        } catch (IOException | JSONException e) {
            Marker fatalMarker = MarkerFactory.getMarker("FATAL");
            LOGGER.error(fatalMarker, "Error while loading configuration file. Please check configuration file exists, or is valid.");
            LOGGER.error(fatalMarker, "Configuration file has to be named \"config.json\" and located at {}", Paths.get(System.getProperty("user.dir"), "data"));
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
