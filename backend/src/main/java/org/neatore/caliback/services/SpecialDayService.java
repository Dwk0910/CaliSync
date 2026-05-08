package org.neatore.caliback.services;

import org.slf4j.MarkerFactory;

import org.springframework.stereotype.Service;

import org.json.XML;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import org.neatore.caliback.CaliBack;
import org.neatore.caliback.object.SpecialDay;
import org.neatore.caliback.object.Date;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class SpecialDayService {
    private JSONObject data;

    private void update(String target_year) {
        try {
            boolean needUpdate = false;

            // 업데이트 필요성 확인
            Path path = CaliBack.datapath;
            if (!path.toFile().exists()) if (!path.toFile().mkdirs()) throw new IOException();

            Path f = Path.of(path.toString(), "special_days.json");
            if (!f.toFile().exists()) {
                needUpdate = true;
                if (!f.toFile().createNewFile()) throw new IOException();
            }

            try {
                Date now = Date.Now.toDate();
                data = new JSONObject(Files.readString(f, StandardCharsets.UTF_8));

                JSONObject data_ = data.getJSONObject(target_year);
                // 마지막 업데이트 날짜로부터 6개월(약 15,768,000초)가 지났을 경우
                if ((System.currentTimeMillis() / 1000) - Long.parseLong(data_.get("updated_at").toString()) > 15_768_000L)
                    needUpdate = true;

                // 또는, 타깃 정보가 올해 정보인데 반해 마지막 업데이트 날짜가 작년 이상으로 과거일 경우
                else if (target_year.equals(now.year) && Integer.parseInt(now.year) > Integer.parseInt(Date.parseDate(data_.get("updated_at").toString()).year))
                    needUpdate = true;

                // tfst, holi, rest, other, anni 중 비어있는 배열이 있는 경우 (정상 응답의 경우 모든 기념일 구분에 데이터가 있어야 함)
                final String[] keys = {"tfst_days", "holi_days", "rest_days", "other_days", "anni_days"};
                for (String key : keys) {
                    if (needUpdate) break;
                    if (data_.optJSONArray(key).isEmpty()) needUpdate = true;
                }
            } catch (JSONException e) {
                // JSON파일에 이상이 있는 경우
                needUpdate = true;
            }

            // update
            if (needUpdate) {
                CaliBack.LOGGER.info("Special data for year {} has expired or does not exist. Updating Special Day database...", target_year);

                JSONObject new_data = new JSONObject();

                // 1) 공휴일: rest
                JSONArray rest_days = getDaysFromURL("getRestDeInfo", target_year);
                new_data.put("rest_days", rest_days);

                // 2) 국경일: holi
                JSONArray holi_days = getDaysFromURL("getHoliDeInfo", target_year);
                new_data.put("holi_days", holi_days);

                // 3) 기념일: anni
                JSONArray anni_days = getDaysFromURL("getAnniversaryInfo", target_year);
                new_data.put("anni_days", anni_days);

                // 4) 24절기: tfst
                JSONArray tfst_days = getDaysFromURL("get24DivisionsInfo", target_year);
                new_data.put("tfst_days", tfst_days);

                // 5) 잡절: other
                JSONArray other_days = getDaysFromURL("getSundryDayInfo", target_year);
                new_data.put("other_days", other_days);

                // updated_at
                new_data.put("updated_at", System.currentTimeMillis() / 1000);

                // Apply to data variable
                if (data == null) data = new JSONObject();
                data.put(target_year, new_data);

                // Apply to file
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(f.toFile()), StandardCharsets.UTF_8)) {
                    writer.write(data.toString(4));
                }
            }
        } catch (IOException e) {
            CaliBack.LOGGER.error(MarkerFactory.getMarker("FATAL"), "SpecialDayService refreshing error", e);
        }
    }

    private JSONArray getDaysFromURL(String serviceName, String target_year) {
        try {
            JSONArray days = new JSONArray();

            String host = "http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/";
            String serviceKey = System.getenv("DATAGOKR_SERVICE_KEY");

            List<CompletableFuture<JSONArray>> queueList = new ArrayList<>();
            for (int month = 1; month <= 12; month++) {
                String str_month = Integer.toString(month).length() == 1 ? "0" + month : Integer.toString(month);
                URL url = new URI(host + serviceName + "?solYear=%s&solMonth=%s&ServiceKey=%s".formatted(target_year, str_month, serviceKey)).toURL();

                CompletableFuture<JSONArray> future = CompletableFuture.supplyAsync(() -> {
                    JSONArray result = new JSONArray();
                    try {
                        URLConnection conn = url.openConnection();
                        conn.setConnectTimeout(3000);
                        conn.setReadTimeout(5000);

                        JSONObject apiResult = XML.toJSONObject(new InputStreamReader(conn.getInputStream())).getJSONObject("response").getJSONObject("body");
                        JSONObject jsonObj = apiResult.optJSONObject("items", new JSONObject());
                        Object object = jsonObj.opt("item");

                        if (object instanceof JSONArray array) {
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject o = array.getJSONObject(i);
                                // YYYYMMDD -> MMDD
                                String locdate_str = Long.toString((o.getLong("locdate") % 10000));
                                o.put("locdate", locdate_str.length() == 3 ? "0" + locdate_str : locdate_str);
                                result.put(o);
                            }
                        } else if (object instanceof JSONObject obj) {
                            // YYYYMMDD -> MMDD
                            String locdate_str = Long.toString((obj.getLong("locdate") % 10000));
                            obj.put("locdate", locdate_str.length() == 3 ? "0" + locdate_str : locdate_str);
                            result.put(obj);
                        }
                    } catch (IOException | JSONException e) {
                        CaliBack.LOGGER.error("SpecialDayService getItemFromURL error : ", e);
                    }
                    return result;
                });
                queueList.add(future);
            }

            queueList.forEach(future -> {
                try {
                    JSONArray futureResult = future.get(10, TimeUnit.SECONDS);
                    futureResult.forEach(days::put);
                } catch (Exception e) {
                    CaliBack.LOGGER.error("SpecialDayService getItemFromURL error : ", e);
                }
            });

            return days;
        } catch (IOException | JSONException | URISyntaxException e) {
            CaliBack.LOGGER.error(MarkerFactory.getMarker("FATAL"), "SpecialDayService getItemFromURL error", e);
            return new JSONArray();
        }
    }

    /**
     * Get Month's special day list
     * @param date - Requires only year and month
     * @return returns list of special days in that month
     */
    public List<SpecialDay> getSpecialDays(Date date, Map<String, SpecialDay> additionals_) {
        if ((2004 > Integer.parseInt(date.year)) || (Integer.parseInt(date.year) > Integer.parseInt(Date.Now.toDate().year) + 1)) return new ArrayList<>();

        update(date.year);

        Set<SpecialDay> specialDaysSet = new LinkedHashSet<>();
        JSONObject data_ = data.getJSONObject(date.year);

        // 'yyyyMM00' -> 'MM'
        String start = date.month;

        // 국경일(holi) > 공휴일(rest) > 기념일(anni) > 24절기(tfst) > 잡절(other)
        List<String> seq = List.of("holi", "rest", "anni", "tfst", "other");

        // 커스텀 기념일들을 type을 기반으로, 이번 달(date.month)에 해당하는 SpecialDay만 분류
        final Map<String, List<SpecialDay>> additionals = Optional.ofNullable(additionals_)
                .map(map -> map.entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith(start))
                        .map(Map.Entry::getValue)
                        .collect(Collectors.groupingBy(SpecialDay::type))
                )
                .orElseGet(Collections::emptyMap);

        for (String s : seq) {
            // From API
            data_.getJSONArray(s + "_days").forEach(o -> {
                JSONObject obj = (JSONObject) o;
                if (obj.get("locdate").toString().startsWith(start)) specialDaysSet.add(new SpecialDay(obj.getString("dateName"), obj.get("locdate").toString(), s));
            });

            // From custom settings
            specialDaysSet.addAll(additionals.getOrDefault(s, Collections.emptyList()));
        }

        // 정렬
        List<SpecialDay> specialDays = new ArrayList<>(specialDaysSet);
        specialDays.sort(Comparator.comparingInt(d -> seq.indexOf(d.type())));

        return specialDays;
    }
}
