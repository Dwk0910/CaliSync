package org.neatore.caliback;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CaliBack {
    public static Logger LOGGER = LogManager.getLogger(CaliBack.class);
    public static void main(String[] args) {
        LOGGER.info("Starting Springboot websocket...");
        SpringApplication.run(CaliBack.class, args);
    }
}
