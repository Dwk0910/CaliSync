package org.neatore.calisync;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.neatore.calisync.service.DBWatcher;
import org.neatore.calisync.util.CalendarProcess;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.platform.win32.WinDef.HWND;

import java.net.URI;

import java.net.URISyntaxException;
import java.nio.file.Path;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class CaliSync {
    public static final Path defaultDir = Path.of(System.getProperty("user.home"), "AppData", "Roaming", "CalendarTask");
    public static final Path process = Path.of(defaultDir.toString(), "desktopcal.exe");

    public static final Path dbDir = Path.of(defaultDir.toString(), "Db");
    public static final Path dbPath = Path.of(dbDir.toString(), "calendar.db");

    public static final String dburl = "jdbc:sqlite:" + dbPath;
    public static final String serverurl = "localhost:8080/calisync";

    public static Logger LOGGER = LogManager.getLogger(CaliSync.class);
    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        if (!dbPath.toFile().exists()) {
            LOGGER.fatal("Could not find CalendarTask database at {}", dbPath.toString());
            return;
        }

        // 캘린더 꺼져있으면 자동으로 실행
        CalendarProcess.refresh();

        LOGGER.info("Opening connection to the CaliSync server...");
        Client client = new Client(new URI("ws://" + serverurl));
        client.connectBlocking(10, TimeUnit.SECONDS);

        // Local DB Watch Service 생성
        LOGGER.info("Registering CaliSync Event Listener...");
        new Thread(new DBWatcher(client)).start();
    }

    public static void connectionError(Throwable e) {
        CaliSync.LOGGER.fatal("", e);

        StringBuilder trace = new StringBuilder();
        Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> trace.append("    at ").append(stackTraceElement.toString()).append("\n"));
        CUser32.INSTANCE.MessageBoxW(null, new WString(" " + e + "\n" + trace), new WString("[Calisync] 서버와의 연결에 실패했습니다."), 0x0000010);

        CalendarProcess.shutdown();
        System.exit(-1);
    }
}

interface CUser32 extends StdCallLibrary {
    CUser32 INSTANCE = Native.load("user32", CUser32.class);
    void MessageBoxW(HWND hWnd, WString IpText, WString IpCaption, int uType);
}
