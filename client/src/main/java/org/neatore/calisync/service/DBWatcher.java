package org.neatore.calisync.service;

import static org.neatore.calisync.CaliSync.LOGGER;
import static org.neatore.calisync.CaliSync.dbDir;
import static org.neatore.calisync.CaliSync.dbPath;
import static org.neatore.calisync.CaliSync.dburl;

import org.neatore.calisync.CaliSync;
import org.neatore.calisync.Client;
import org.neatore.calisync.object.Date;
import org.neatore.calisync.object.Day;
import org.neatore.calisync.object.Schedule;
import org.neatore.calisync.packet.SignalPacket;
import org.neatore.calisync.util.CalendarProcess;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.charset.StandardCharsets;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

import org.json.JSONObject;
import org.json.JSONArray;

public class DBWatcher implements Runnable {
    public static DBWatcher instance;

    private final Client client;
    public DBWatcher(Client client) {
        instance = this;
        this.client = client;
    }

    public static DBWatcher getInstance() {
        return instance;
    }

    @Override
    public void run() {
        // initial update
        CaliSync.LOGGER.info("Performing initial database sync...");
        this.initialUpdate();

        // 초기화 업데이트가 실행되도록 대기
        synchronized (ignore) {
            try {
                while (ignore.get()) ignore.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            // Desktop Calendar는 일정 수정 시 기존 DB를 덮어쓰므로 ENTRY_CREATE로 쓰기 이벤트 감지
            dbDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            LOGGER.info("** STANDBY **");
            while (true) {
                WatchKey key = watchService.take();

                if (ignore.get()) {
                    key.pollEvents(); // ignore 상태이므로 이벤트 버리기
                    if (!key.reset()) break;
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    // 저널 파일이 먼저 들어오므로, 저널 파일로 검사
                    String fileName = ((Path) event.context()).getFileName().toString().replace("-journal", "");

                    // 파일 변경 감지
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE && fileName.equals(dbPath.getFileName().toString())) {
                        // 이미 처리 중이면 플래그 설정 후 종료
                        if (isProcessing.get()) hasPendingChange.set(true);
                        else {
                            isProcessing.set(true);
                            hasPendingChange.set(false);
                            LOGGER.info("Database change detected. Sending signal to server...");
                            onFileChange();
                        }
                    }
                }

                // 다음 이벤트 감지를 위해 키 재설정
                if (!key.reset()) break;
            }
        } catch (IOException | InterruptedException e) {
            CaliSync.LOGGER.fatal(e);
        }
    }

    // 중복 호출 방지용 플래그
    public static final AtomicBoolean isProcessing = new AtomicBoolean(false),
            hasPendingChange = new AtomicBoolean(false),
            ignore = new AtomicBoolean(false);
    private void onFileChange() {
        // 서버로부터 마지막 수정 날짜와 전체 레코드 수 받아와서 update()로 전달
        JSONObject res = client.sendSignalWithResponse(new SignalPacket(SignalPacket.Method.UPDATE_INFO, null)).exceptionally(e -> {
            isProcessing.set(false);
            LOGGER.error("Failed to receive database update signal: {}", e);
            return null;
        }).join();

        try { this.update(res); } finally {
            isProcessing.set(false);
            // 처리 중에 변경 사항이 또 있었는지 확인하고 있으면 재호출
            if (hasPendingChange.get()) onFileChange();
        }
    }

    public void update(JSONObject serverData) {
        JSONObject data = serverData.getJSONObject("body");
        String lastModified = data.getString("last_modified");
        List<Day> targets = new ArrayList<>();

        // DB에 뭔짓거리 했음
        boolean did_something_to_db = false;

        long insertCount = 0L;
        // 서버에서의 마지막 수정 날짜 이후의 일정들만 조회 및 등록
        try (Connection conn = DriverManager.getConnection(dburl);
             PreparedStatement pstmt = conn.prepareStatement("SELECT it_unique_id, it_bgcolor, it_content, it_history, it_cdate, it_mdate FROM item_table WHERE it_mdate > ?;")
        ) {
            pstmt.setString(1, lastModified);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Date date = Date.parseDate(rs.getString("it_unique_id").replace("dkcal_mdays_", ""));

                List<Schedule> schedules = new ArrayList<>();

                for (String i : rs.getString("it_content").split("\n")) {
                    Schedule schedule = new Schedule(date, i);
                    schedules.add(schedule);
                }

                Date it_cdate = Date.parseDate(rs.getString("it_cdate")),
                        it_mdate = Date.parseDate(rs.getString("it_mdate"));

                if (it_cdate.equals(it_mdate)) insertCount++;

                Day day = new Day(date, schedules, it_cdate, it_mdate, rs.getString("it_bgcolor"), rs.getString("it_history"));
                targets.add(day);
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }

        // 만약 false이면 변경사항이 없는 것 (캘린더 재시작 과정에서 저널 파일을 감지한 것임)
        if (!targets.isEmpty()) {
            // 서버로 변경된 일정들 전송
            JSONObject obj = new JSONObject();
            JSONArray array = new JSONArray();
            for (Day day : targets) {
                array.put(day.toJSONObject());
            }
            obj.put("targets", array);
            client.sendSignalWithResponse(new SignalPacket(SignalPacket.Method.UPDATE, obj.toMap())).thenAccept(res -> {
                if (res.getInt("code") != 200) LOGGER.error("Failed to update database on server: {}", res.toString(4));
            });
        }


        // 서버의 totalCount는 기존 값에 이번에 추가된 일정 수를 더한 값으로 설정 (추가가 아닌 수정은 제외시켜야 함)
        long totalCount = Long.parseLong(data.getString("total_count")) + insertCount;

        // total_count가 다르면 전체 동기화 필요

        try (Connection conn = DriverManager.getConnection(dburl);
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) AS count FROM item_table;")
        ) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                long localTotalCount = Long.parseLong(rs.getString("count"));
                if (localTotalCount != totalCount) {
                    LOGGER.info("Total count mismatch (local: {}, server: {}).", localTotalCount, totalCount);
                    // 전체 동기화 수행
                    client.sendSignalWithResponse(new SignalPacket(SignalPacket.Method.HARD_UPDATE, null))
                            .thenApply(res -> res.getString("body"))
                            .thenAcceptAsync(this::hardUpdate);
                    did_something_to_db = true;
                    return;
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }

        // DB 변경사항 있으면 적용
        if (did_something_to_db) CalendarProcess.refresh();
    }

    private void hardUpdate(String key, boolean... isManual) {
        // HARD UPDATE 중간에는 Calendar 강제 종료 및 DB 잠금(ignore)
        CalendarProcess.shutdown();
        ignore.set(true);

        try {
            LOGGER.info("HARD UPDATE : Downloading hard update SQL data from server...");
            URL url = new URI("http://" + CaliSync.serverurl + "/hard-update/" + key).toURL();

            // 서버로부터 파일 다운로드 및 저장
            HttpURLConnection httpconn = (HttpURLConnection) url.openConnection();
            try (InputStream in = httpconn.getInputStream()) {
                try (GZIPInputStream gzipin = new GZIPInputStream(in);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(gzipin, StandardCharsets.UTF_8));
                     Connection conn = DriverManager.getConnection(dburl)
                ) {
                    // 트랜젝션 처리
                    conn.setAutoCommit(false);

                    // read JSON
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);

                    // Apply
                    JSONArray array = new JSONArray(sb.toString());

                    try (PreparedStatement delete_all = conn.prepareStatement("DELETE FROM item_table");
                         PreparedStatement pstmt = conn.prepareStatement("INSERT INTO item_table (u_mid, it_unique_id, it_bgcolor, it_content, it_history, it_cdate, it_mdate, it_mtime) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                        // 초기화
                        delete_all.execute();

                        for (Object o : array) {
                            JSONObject obj = (JSONObject) o;
                            pstmt.setString(1, obj.getString("u_mid"));
                            pstmt.setString(2, obj.getString("it_unique_id"));
                            pstmt.setString(3, obj.getString("it_bgcolor"));
                            pstmt.setString(4, obj.getString("it_content"));
                            pstmt.setString(5, obj.getString("it_history"));
                            pstmt.setString(6, obj.getString("it_cdate"));
                            pstmt.setString(7, obj.getString("it_mdate"));
                            pstmt.setString(8, Integer.toString(obj.getInt("it_mtime")));
                            pstmt.addBatch();
                        }

                        pstmt.executeBatch();
                        conn.commit();
                        LOGGER.info("HARD UPDATE completed successfully.");
                    }
                }
            }
            httpconn.disconnect();
        } catch (URISyntaxException | SQLException | IOException e) {
            LOGGER.error(e);
        }

        // 모든 데이터가 동기화되었으므로 queue 비우기 및 ignore 해제
        hasPendingChange.set(false);
        if (isManual.length > 0 && !isManual[0]) ignore.set(false);

        // 변경사항 적용
        CalendarProcess.refresh();
    }

    public void initialUpdate() {
        try {
            // WatchService registering 저지
            ignore.set(true);

            // 모든 동작은 watcherservice 등록 전에 완료해야 하므로 동기적으로 처리한다.
            // 데이터의 기준은 서버가 되어야 하므로, WatchService가 켜지기 전의 모든 내용은 소실되고 서버와 동기화시킨다.
            // clientCount를 받고 바로 try문을 닫아야 다음에 hard update를 할 때 db lock 에러가 뜨지 않는다.
            String clientLstMdate = "", serverLstMdate = client.sendSignalWithResponse(new SignalPacket(SignalPacket.Method.UPDATE_INFO, null)).join().getJSONObject("body").getString("last_modified");
            try (Connection conn = DriverManager.getConnection(dburl);
                 PreparedStatement pstmt = conn.prepareStatement("SELECT MAX(it_mdate) AS last_modified FROM item_table;")
            ) {
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) clientLstMdate = rs.getString("last_modified");
            } catch (SQLException e) {
                CaliSync.LOGGER.fatal(e);
                System.exit(-1);
            }

            if (!clientLstMdate.equals(serverLstMdate)) {
                JSONObject res = client.sendSignalWithResponse(new SignalPacket(SignalPacket.Method.HARD_UPDATE, null)).join();
                this.hardUpdate(res.getString("body"), true);
            }
        } finally {
            // 레지스터링 재개
            synchronized (ignore) {
                ignore.set(false);
                ignore.notifyAll();
            }
        }
    }

    public void synchronize() {
        JSONObject res = client.sendSignalWithResponse(new SignalPacket(SignalPacket.Method.HARD_UPDATE, null)).join();
        this.hardUpdate(res.getString("body"));
    }
}
