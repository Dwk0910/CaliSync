package org.neatore.calisync.service;

import static org.neatore.calisync.CaliSync.LOGGER;
import static org.neatore.calisync.CaliSync.dbDir;
import static org.neatore.calisync.CaliSync.dbPath;

import org.json.JSONArray;
import org.neatore.calisync.CaliSync;
import org.neatore.calisync.Client;
import org.neatore.calisync.object.Date;
import org.neatore.calisync.object.Day;
import org.neatore.calisync.object.Schedule;
import org.neatore.calisync.packet.SignalPacket;

import java.io.IOException;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;

public class DBWatcher implements Runnable {
    private final Client client;
    public DBWatcher(Client client) {
        this.client = client;
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            // Desktop Calendar는 일정 수정 시 기존 DB를 덮어쓰므로 ENTRY_CREATE로 쓰기 이벤트 감지
            dbDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            LOGGER.info("** STANDBY **");
            while (true) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    // 저널 파일이 먼저 들어오므로, 저널 파일로 검사
                    String fileName = ((Path) event.context()).getFileName().toString().replace("-journal", "");

                    // 파일 변경 감지
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE && fileName.equals(dbPath.getFileName().toString())) {
                        // 이미 처리 중이면 플래그 설정 후 종료
                        if (isProcessing.get()) hasPendingChange.set(true);
                        else onFileChange();
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
    private final AtomicBoolean isProcessing = new AtomicBoolean(false),
            hasPendingChange = new AtomicBoolean(false);
    private void onFileChange() {
        isProcessing.set(true);
        hasPendingChange.set(false);

        // 서버로부터 마지막 수정 날짜와 전체 레코드 수 받아와서 update()로 전달
        LOGGER.info("Database change detected. Sending signal to server...");
        client.sendSignalWithResponse(new SignalPacket(SignalPacket.Method.UPDATE_INFO, null)).thenAccept(res -> {
            try {
                this.update(res);
            } finally {
                isProcessing.set(false);
                // 처리 중에 변경 사항이 또 있었는지 확인하고 있으면 재호출
                if (hasPendingChange.get()) onFileChange();
            }
        }).exceptionally(e -> {
            isProcessing.set(false);
            LOGGER.error("Failed to receive database update signal: {}", e);
            return null;
        });
    }

    public void update(JSONObject serverData) {
        JSONObject data = serverData.getJSONObject("body");

        String lastModified = data.getString("last_modified"),
                totalCount = data.getString("total_count");

        List<Day> targets = new ArrayList<>();

        // 서버에서의 마지막 수정 날짜 이후의 일정들만 조회 및 등록
        String dburl = "jdbc:sqlite:" + dbPath;
        try (Connection conn = DriverManager.getConnection(dburl);
             PreparedStatement pstmt = conn.prepareStatement("SELECT it_unique_id, it_bgcolor, it_content, it_history, it_mdate FROM item_table WHERE it_mdate > ?;")
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

                Day day = new Day(date, schedules, Date.parseDate(rs.getString("it_mdate")), rs.getString("it_bgcolor"), rs.getString("it_history"));
                targets.add(day);
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }

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

        // total_count가 다르면 전체 동기화 필요
//        try (Connection conn = DriverManager.getConnection(dburl);)
//             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) AS count FROM item_table;")
//        ) {
//            ResultSet rs = pstmt.executeQuery();
//            if (rs.next()) {
//                String localTotalCount = rs.getString("count");
//                if (!localTotalCount.equals(totalCount)) {
//                    LOGGER.info("Total record count mismatch (local: {}, server: {}). Triggering full sync...", localTotalCount, totalCount);
//                    client.sendSignalWithResponse(new SignalPacket(SignalPacket.Method.FULL_SYNC, null)).thenAccept(res -> {
//                        if (res.getInt("code") != 200) LOGGER.error("Failed to trigger full sync on server: {}", res.toString(4));
//                    });
//                }
//            }
//        } catch (SQLException e) {
//            LOGGER.error(e);
//        }
    }
}
