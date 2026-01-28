package org.neatore.caliback.controller;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.neatore.caliback.CaliBack;
import org.neatore.caliback.object.Date;
import org.neatore.caliback.object.SpecialDay;
import org.neatore.caliback.services.SpecialDayService;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@CrossOrigin(origins = { "http://localhost:5173" })
@RequestMapping("/webservice")
public class WebServiceController {
    private final SpecialDayService specialDayService;
    private Map<SpecialDay, SpecialDay> replaceTargets = null;
    private Map<String, SpecialDay> additions = null;

    public WebServiceController(SpecialDayService specialDayService) {
        this.specialDayService = specialDayService;

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
            CaliBack.LOGGER.warn("Failed to load custom settings for Special Days. Your custom settings will not be loaded.", e);
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

        // TODO: Schedules

        result.put("schedules", new JSONArray());

        return ResponseEntity.ok(result.toString(4));
    }
}
