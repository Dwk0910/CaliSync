package org.neatore.caliback.services;

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
import java.net.URISyntaxException;

import java.util.LinkedHashSet;
import java.util.Set;

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
                if (target_year.equals(now.year) && Integer.parseInt(now.year) > Integer.parseInt(Date.parseDate(data_.get("updated_at").toString()).year))
                    needUpdate = true;
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
            CaliBack.LOGGER.fatal("SpecialDayService refreshing error : {}", e);
        }
    }

    private JSONArray getDaysFromURL(String serviceName, String target_year) {
        try {
            JSONArray days = new JSONArray();

            String host = "http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/";
            String serviceKey = System.getenv("DATAGOKR_SERVICE_KEY");

            for (int month = 1; month <= 12; month++) {
                String str_month = Integer.toString(month).length() == 1 ? "0" + month : Integer.toString(month);
                URL url = new URI(host + serviceName + "?solYear=%s&solMonth=%s&ServiceKey=%s".formatted(target_year, str_month, serviceKey)).toURL();

                JSONObject obj;
                try {
                    obj = XML.toJSONObject(new InputStreamReader(url.openConnection().getInputStream())).getJSONObject("response").getJSONObject("body").getJSONObject("items");
                } catch (JSONException e) {
                    continue;
                }

                try {
                    JSONArray item = obj.getJSONArray("item");
                    for (Object o : item) {
                        JSONObject _o = (JSONObject) o;
                        days.put(_o);
                    }
                } catch (JSONException e) {
                    JSONObject item = obj.getJSONObject("item");
                    days.put(item);
                }
            }

            return days;
        } catch (IOException | JSONException | URISyntaxException e) {
            CaliBack.LOGGER.fatal("SpecialDayService getItemFromURL error : ", e);
            return new JSONArray();
        }
    }

    /**
     * Get Month's special day list
     * @param date - Requires only year and month
     * @return returns list of special days in that month
     */
    public Set<SpecialDay> getSpecialDays(Date date) {
        if ((2004 > Integer.parseInt(date.year)) || (Integer.parseInt(date.year) > Integer.parseInt(Date.Now.toDate().year) + 1)) return new LinkedHashSet<>();

        update(date.year);

        Set<SpecialDay> specialDays = new LinkedHashSet<>();
        JSONObject data_ = data.getJSONObject(date.year);

        // 'yyyyMM'
        String start = date.getDate(1).substring(0, 6);

        // 국경일(holi) > 공휴일(rest) > 기념일(anni) > 24절기(tfst) > 잡절(other)
        String[] seq = {"holi", "rest", "anni", "tfst", "other"};

        for (String s : seq) {
            data_.getJSONArray(s + "_days").forEach(o -> {
                JSONObject obj = (JSONObject) o;
                if (obj.get("locdate").toString().startsWith(start)) specialDays.add(new SpecialDay(obj.getString("dateName"), obj.get("locdate").toString(), s));
            });
        }

        return specialDays;
    }
}
