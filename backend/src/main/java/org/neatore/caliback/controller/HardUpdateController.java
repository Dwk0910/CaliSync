package org.neatore.caliback.controller;

import org.json.JSONArray;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;

import java.nio.charset.StandardCharsets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

import static org.neatore.caliback.CaliBack.LOGGER;
import static org.neatore.caliback.CaliBack.dbc;

@RestController
@RequestMapping("/caliclient")
public class HardUpdateController {
    public static final Map<String, Long> VALID_KEYS = new ConcurrentHashMap<>();

    @GetMapping("/hard-update/{key}")
    public ResponseEntity<Resource> getSQLFile(@PathVariable String key) {
        Long expiry = VALID_KEYS.remove(key);
        if (expiry == null || System.currentTimeMillis() > expiry)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // DB 데이터를 JSON으로 뽑아 압축해 전송
        try {
            List<Map<String, Object>> dataList = new ArrayList<>();
            try (Connection conn = DriverManager.getConnection(dbc.dburl());
                 PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM item_table");
                 ResultSet rs = pstmt.executeQuery()) {

                ResultSetMetaData rsmd = rs.getMetaData();
                int colCount = rsmd.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(rsmd.getColumnName(i), rs.getObject(i));
                    }
                    dataList.add(row);
                }
            }

            JSONArray array = new JSONArray(dataList);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                gzip.write(array.toString(4).getBytes(StandardCharsets.UTF_8));
            }

            ByteArrayResource resource = new ByteArrayResource(out.toByteArray());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/x-gzip"))
                    .body(resource);
        } catch (Exception e) {
            LOGGER.error("", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
