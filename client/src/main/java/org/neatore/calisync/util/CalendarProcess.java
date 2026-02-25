package org.neatore.calisync.util;

import org.neatore.calisync.CaliSync;
import org.neatore.calisync.service.DBWatcher;

import static org.neatore.calisync.CaliSync.process;

public class CalendarProcess {
    public static void refresh() {
        try {
            // Watcher 비활성화
            DBWatcher.ignore.set(true);

            // 실행 중인지 확인하고 종료
            if (isRunning()) new ProcessBuilder("taskkill", "/F", "/IM", process.getFileName().toString()).start().waitFor();

            // 다시 실행
            new ProcessBuilder(process.toString()).start();

            // 10초간 .1초 간격으로 켜짐 확인
            float attempts = 0f;
            while (attempts < 10f) {
                if (isRunning()) {
                    // 1초 유예 (저널파일 생성)
                    Thread.sleep(1000);

                    // Watcher 활성화
                    DBWatcher.ignore.set(false);
                    return;
                }
                Thread.sleep(100);
                attempts += .1f;
            }
            throw new Exception("Calendar Timed Out : Tried to start calendar 100 times. but Calendar didn't respond.");
        } catch (Exception e) {
            CaliSync.LOGGER.error("", e);
        }
    }

    public static void shutdown() {
        try {
            if (!isRunning()) return;
            new ProcessBuilder("taskkill", "/F", "/IM", process.getFileName().toString()).start().waitFor();
        } catch (Exception e) {
            CaliSync.LOGGER.error("", e);
        }
    }

    public static boolean isRunning() {
        return ProcessHandle.allProcesses()
                .map(ProcessHandle::info)
                .anyMatch(info -> info.command().map(cmd -> cmd.contains(process.getFileName().toString())).orElse(false));
    }
}
