package org.neatore.calisync;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.neatore.calisync.util.CalendarProcess;
import org.neatore.calisync.util.NotifySystem;

import java.nio.file.Path;

import java.util.Arrays;

public class CaliSync {

    public static final Path defaultDir = Path.of(System.getProperty("user.home"), "AppData", "Roaming", "CalendarTask");
    public static final Path process = Path.of(defaultDir.toString(), "desktopcal.exe");

    public static final Path dbDir = Path.of(defaultDir.toString(), "Db");
    public static final Path dbPath = Path.of(dbDir.toString(), "calendar.db");

    public static final String dburl = "jdbc:sqlite:" + dbPath;
    public static final String serverurl = "neatorebackend.kro.kr/calisync";
    public static final String serverurl_local = "localhost:8080";
    public static String url;

    public static Logger LOGGER = LogManager.getLogger(CaliSync.class);
    public static NotifySystem notifySystem = new NotifySystem();

    public static void main(String[] args) {
        if (!dbPath.toFile().exists()) {
            LOGGER.fatal("Could not find CalendarTask database at {}", dbPath.toString());
            return;
        }

        // 캘린더 꺼져있으면 자동으로 실행
        CalendarProcess.refresh();

        LOGGER.info("Process started. Opening connection to the CaliSync backend SSE Server...");


        boolean localtest = Arrays.stream(args).toList().contains("-LOCALTEST");
        CaliSync.url = ((localtest) ? "http://" + serverurl_local : "https://" + serverurl);

        SSEClient client = new SSEClient();
        client.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("자원 정리중...");
            client.disconnect();
        }));
    }


//    public static void main(String[] args) throws URISyntaxException, InterruptedException {
//        if (!dbPath.toFile().exists()) {
//            LOGGER.fatal("Could not find CalendarTask database at {}", dbPath.toString());
//            return;
//        }
//
//        // 캘린더 꺼져있으면 자동으로 실행
//        CalendarProcess.refresh();
//
//        LOGGER.info("Opening connection to the CaliSync server...");
//        boolean localtest = Arrays.stream(args).toList().contains("-LOCALTEST");
//        Client client = new Client(new URI(((localtest) ? "ws://" : "wss://") + ((localtest) ? serverurl_local : serverurl) + "/caliclient"));
//
//        client.connectBlocking(10, TimeUnit.SECONDS);
//
//        if (!client.isOpen()) {
//            notifySystem.openErrorWindow("[CaliSync] 초기 연결 오류", "초기 연결 실패 (인증 실패일 수 있습니다)");
//            return;
//        }
//
//        // Local DB Watch Service 생성
//        LOGGER.info("Registering CaliSync Event Listener...");
//        new Thread(new DBWatcher(client)).start();
//    }

    public static int connectionError(Throwable e) {
        CaliSync.LOGGER.fatal("", e);

        StringBuilder trace = new StringBuilder();
        Arrays.stream(e.getStackTrace()).forEach(stackTraceElement -> trace.append("        at ").append(stackTraceElement.toString()).append("\n"));
        return notifySystem.openErrorWindowRetry("[CaliSync] 서버와의 연결에 실패했습니다.", e + "\n" + trace);
    }
}
