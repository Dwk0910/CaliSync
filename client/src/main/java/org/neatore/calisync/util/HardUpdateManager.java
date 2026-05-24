package org.neatore.calisync.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.zip.GZIPInputStream;

import java.nio.charset.StandardCharsets;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.neatore.calisync.CaliSync.LOGGER;
import static org.neatore.calisync.CaliSync.dburl;
import static org.neatore.calisync.CaliSync.url;

public class HardUpdateManager {
    private final OkHttpClient client = new OkHttpClient();
    private final Request request = new Request.Builder()
            .url(url + "/caliclient/hard-update/getKey")
            .addHeader("Authorization", System.getenv("CALISYNC_CLIENT_SECRET"))
            .build();

    public void run() {
        try {
            LOGGER.info("HARD UPDATE : Downloading hard update SQL data from server...");

            String key = null;

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) key = response.body().string();
                else throw new IOException();
            } catch (IOException e) {
                LOGGER.error("Error while hard-updating the local DB.", e);
            }

            URL updateURL = new URI(url + "/caliclient/hard-update/" + key).toURL();

            // 서버로부터 파일 다운로드 및 저장
            HttpURLConnection httpconn = (HttpURLConnection) updateURL.openConnection();
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
    }
}
