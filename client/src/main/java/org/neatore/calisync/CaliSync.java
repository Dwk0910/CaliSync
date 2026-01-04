package org.neatore.calisync;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.neatore.calisync.packet.SignalPacket;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
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

        Scanner scan = new Scanner(System.in);
        System.out.print("type date (yyyyMMdd) : ");
        String date = scan.nextLine();

        Map<String, Object> obj = new HashMap<>();
        obj.put("date", date);
        obj.put("content", "Test schedule");

        client.sendSignal(new SignalPacket(SignalPacket.Method.POST, obj));
    }
}
