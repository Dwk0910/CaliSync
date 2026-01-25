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

import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@RestController
@CrossOrigin(origins = { "http://localhost:5173" })
@RequestMapping("/webservice")
public class WebServiceController {
    private final SpecialDayService specialDayService;
    public WebServiceController(SpecialDayService specialDayService) {
        this.specialDayService = specialDayService;
    }

    @GetMapping("/specialdays/{year}/{month}")
    public ResponseEntity<String> getSpecialDays(@PathVariable String year, @PathVariable String month) {
        final AtomicReference<Map<String, Object>> customData = new AtomicReference<>(null);
        final AtomicReference<JSONArray> additionally = new AtomicReference<>(new JSONArray());

        // parsing custom data
        try {
            JSONObject customData_ = new JSONObject(Files.readString(Path.of(CaliBack.datapath.toString(), "custom_settings.json")));
            customData.set(customData_.toMap());
            additionally.set(customData_.getJSONArray("additionally"));
        } catch (IOException | JSONException ignored) {}

        // serviceResult <- customData + additionally
        Set<SpecialDay> serviceResult = specialDayService.getSpecialDays(new Date(year, month, "0"));
        JSONArray result = new JSONArray(Stream.concat(
                serviceResult
                        .stream()
                        .map(d -> {
                            JSONObject i = new JSONObject();
                            // MMdd
                            String custom_data_search_query = d.date().substring(4, 8);

                            if (customData.get() != null && customData.get().containsKey(custom_data_search_query)) i.put("name", customData.get().get(custom_data_search_query).toString());
                            else i.put("name", d.name());

                            i.put("date", d.date());
                            i.put("type", d.type());

                            return i;
                        }),
                additionally.get().toList()
                        .stream()
                        .map(d -> {
                            // ** RETURN NULL IF THERE IS NO DATA TO RETURN **

                            // 선행 과정에서 에러가 발생한 것.
                            if (serviceResult.isEmpty()) return null;

                            JSONObject i = new JSONObject();

                            // Object d parsing
                            if (d instanceof Map<?, ?> raw) {
                                raw.forEach((k, v) -> i.put(k.toString(), v));
                            }

                            JSONObject j = null;

                            // MMdd -> MM
                            String custom_data_search_query = i.getString("date").substring(0, 2);

                            if (custom_data_search_query.equals(month.length() == 1 ? "0" + month : month)) {
                                j = new JSONObject();
                                j.put("name", i.getString("name"));
                                j.put("date", year + i.getString("date"));
                                j.put("type", i.getString("type"));
                            }

                            return j;
                        })
                        .filter(Objects::nonNull)
        ).toList());

        return ResponseEntity.ok(result.toString(4));
    }
}
