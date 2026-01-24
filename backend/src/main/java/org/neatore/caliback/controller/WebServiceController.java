package org.neatore.caliback.controller;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.neatore.caliback.object.Date;
import org.neatore.caliback.services.SpecialDayService;

@RestController
@RequestMapping("/webservice")
public class WebServiceController {
    private final SpecialDayService specialDayService;
    public WebServiceController(SpecialDayService specialDayService) {
        this.specialDayService = specialDayService;
    }

    @GetMapping("/specialdays/{year}/{month}")
    public ResponseEntity<String> getSpecialDays(@PathVariable String year, @PathVariable String month) {
        JSONArray serviceResult = new JSONArray(specialDayService.getSpecialDays(new Date(year, month, "0"))
                .stream()
                .map(
                        d -> new JSONObject()
                                .put("name", d.name())
                                .put("date", d.date())
                                .put("type", d.type())
                ).toList()
        );
        return ResponseEntity.ok(serviceResult.toString(4));
    }
}
