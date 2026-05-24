package org.neatore.calisync;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import org.json.JSONObject;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import org.neatore.calisync.util.CalendarProcess;
import org.neatore.calisync.util.HardUpdateManager;

import java.util.concurrent.TimeUnit;

import static org.neatore.calisync.CaliSync.LOGGER;
import static org.neatore.calisync.CaliSync.url;

public class SSEClient {
    private final OkHttpClient client;
    private final Request request;
    private EventSource currentEventSource;

    private final HardUpdateManager hum;

    private final int MAX_RETRIES = 10;
    private int retries = 0;

    public SSEClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .build();

        this.request = new Request.Builder()
                // 웹 서비스와 동기화해야 하므로 CaliBack 웹 서비스 컨트롤러 내에 있는 AutoRefereshEventSource를 구독하도록 설정
                .url(url + "/webservice/autoRefereshEventSource")
                .header("Accept", "text/event-stream")
                .header("Authorization", System.getenv("CALISYNC_CLIENT_SECRET"))
                .build();

        this.hum = new HardUpdateManager();

        // initial hard update (Synchronizing with the server at startup)
        this.hum.run();
    }

    public void start() {
        this.currentEventSource = EventSources.createFactory(client).newEventSource(request, new EventSourceListener() {
            @Override
            public void onOpen(@NonNull EventSource eventSource, @NonNull Response response) {
                LOGGER.info("Connection established.");
            }

            @Override
            public void onEvent(@NonNull EventSource eventSource, @Nullable String id, @Nullable String type, @NonNull String data) {
                JSONObject res = new JSONObject(data);

                // case 0 (session id)는 캘린더 수정 시에 서버 전송용으로 필요. CaliClient는 read-only이므로 사용하지 않음
                switch (res.getInt("code")) {
                    case 0 -> retries = 0;
                    case 600 -> {
                        LOGGER.info("Received calendar update event. Refreshing calendar...");
                        hum.run();
                        CalendarProcess.refresh();
                    }
                }
            }

            @Override
            public void onClosed(@NonNull EventSource eventSource) {
                LOGGER.info("Connection closed.");
            }

            @Override
            public void onFailure(@NonNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                eventSource.cancel();

                if (retries < MAX_RETRIES) {
                    retries++;

                    long backoffTime = (long) Math.pow(2, retries) * 1000;
                    LOGGER.error("오류 발생: {}. {}", (t != null ? t.getMessage() : "Unknown error"), backoffTime / 1000 + "초 후에 재시도합니다...");

                    try {
                        Thread.sleep(backoffTime);
                        start();
                    } catch (InterruptedException ignored) {
                    }
                } else LOGGER.error("재연결 시도 횟수 초과");
            }
        });
    }

    public void disconnect() {
        if (currentEventSource != null) {
            currentEventSource.cancel();
            currentEventSource = null;
        }

        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}
