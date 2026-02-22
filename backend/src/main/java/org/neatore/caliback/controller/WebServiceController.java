package org.neatore.caliback.controller;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.neatore.caliback.CaliBack;
import org.neatore.caliback.services.AutoUpdateService;
import org.neatore.caliback.services.SpecialDayService;
import org.neatore.caliback.services.DBCService;
import org.neatore.caliback.object.Date;
import org.neatore.caliback.object.SpecialDay;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webservice")
public class WebServiceController {
    private final AutoUpdateService autoUpdateService;
    private final SpecialDayService specialDayService;
    private final DBCService dbcService;

    private Map<SpecialDay, SpecialDay> replaceTargets = null;
    private Map<String, SpecialDay> additions = null;

    public WebServiceController(AutoUpdateService autoUpdateService, SpecialDayService specialDayService, DBCService dbcService) {
        this.autoUpdateService = autoUpdateService;
        this.specialDayService = specialDayService;
        this.dbcService = dbcService;

        // parsing custom data
        try {
            final JSONObject customFileData = new JSONObject(Files.readString(Path.of(CaliBack.datapath.toString(), "custom_settings.json")));

            if (customFileData.has("replace")) {
                JSONArray repArray = customFileData.getJSONArray("replace");
                Map<SpecialDay, SpecialDay> map = new HashMap<>();
                for (Object o : repArray) {
                    JSONObject obj = (JSONObject) o;
                    SpecialDay from = new SpecialDay(obj.getJSONObject("from"));
                    SpecialDay to = new SpecialDay(obj.getJSONObject("to"));

                    map.put(from, to);
                }
                this.replaceTargets = map;
            }

            if (customFileData.has("additionally")) {
                JSONArray addArray = customFileData.getJSONArray("additionally");
                Map<String, SpecialDay> map = new HashMap<>();
                for (Object o : addArray) {
                    JSONObject obj = (JSONObject) o;
                    SpecialDay sd = new SpecialDay(obj);
                    map.put(sd.date(), sd);
                }
                this.additions = map;
            }
        } catch (IOException | JSONException e) {
            if (e instanceof NoSuchFileException) CaliBack.LOGGER.info("Custom setting file does not exist.");
            else CaliBack.LOGGER.warn("Failed to load custom settings for Special Days. Your custom settings will not be loaded.", e);
        }
    }

    @GetMapping("/getMonthInfo/{year}/{month}")
    public ResponseEntity<String> getSpecialDays(@PathVariable String year, @PathVariable String month) {
        JSONObject result = new JSONObject();

        // Special Days
        Date date = new Date(year, month, "00");
        List<SpecialDay> serviceResult = specialDayService.getSpecialDays(date, additions);

        if (replaceTargets != null) serviceResult = serviceResult.stream().map(sd -> replaceTargets.getOrDefault(sd, sd)).toList();

        JSONArray spd_result = new JSONArray(serviceResult
                .stream()
                .map(d -> {
                    JSONObject obj = new JSONObject();
                    obj.put("name", d.name());
                    obj.put("date", d.date());
                    obj.put("type", d.type());
                    return obj;
                })
                .toList()
        );

        result.put("specialDays", spd_result);

        JSONObject data = new JSONObject();
        data.put("date", date.getDate(1));

        String resultstr = dbcService.process(new JSONObject().put("method", "GET_MONTH").put("data", data)).getResponseBody().toString();
        JSONArray array = new JSONArray(resultstr);

        result.put("schedules", array);

        return ResponseEntity.ok(result.toString(4));
    }

    @PostMapping("/setSchedules")
    public ResponseEntity<String> setSchedules(@RequestHeader("X-Client-ID") String sessionId, @RequestBody Map<String, Object> req) {
        Date date = Date.parseDate(req.get("date").toString());

        StringBuilder content = new StringBuilder();
        List<?> schedules_ = (List<?>) req.get("schedules");
        schedules_.forEach(i -> {
            Map<?, ?> schedule = (Map<?, ?>) i;
            if (!content.isEmpty()) content.append("\n");
            content.append(schedule.get("content").toString());
        });

        JSONObject data = new JSONObject();
        data.put("date", date.getDate(1));
        data.put("content", content.toString());

        dbcService.process(new JSONObject().put("method", "POST_SET").put("data", data));

        // 이 동작은 전체 리로드를 요함
        autoUpdateService.trigger(sessionId);

        return ResponseEntity.ok().build();
    }
}
