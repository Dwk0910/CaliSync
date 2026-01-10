package org.neatore.caliback.object;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Date {
    private final String year, month, day;
    private String hour, minute, second;

    public Date(String year, String month, String day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    // ** DEFUALT ZONE ID **
    private static final ZoneId timeZone = ZoneId.of("Asia/Seoul");

    public static final class Now {
        public static Date toDate() {
            Instant instant = Instant.now();
            ZonedDateTime zdt = instant.atZone(timeZone);
            return new Date(Integer.toString(zdt.getYear()), Integer.toString(zdt.getMonthValue()), Integer.toString(zdt.getDayOfMonth()));
        }

        public static String getUnixTime() {
            return Long.toString(Instant.now().getEpochSecond());
        }

        public static String format(String format) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            return Instant.ofEpochSecond(Long.parseLong(getUnixTime()))
                    .atZone(timeZone)
                    .format(formatter);
        }
    }

    /**
     * String format must be one of the following.
     * <ul>
     *  <li>'yyyy-MM-dd HH:mm:ss'</li>
     *  <li>'yyyy-MM-dd'</li>
     *  <li>'yyyyMMdd'</li>
     *  <li>or Unix timestamp [UTC]</li>
     * </ul>
     */
    public static Date parseDate(String s) {
        if (s == null || s.isEmpty()) return null;

        String year, month, day;
        String hour = "00", minute = "00", second = "00";

        if (s.contains("-") && s.contains(":")) { // 1. 'yyyy-MM-dd HH:mm:ss'
            String[] parts = s.split(" ");
            String[] dateParts = parts[0].split("-");
            String[] timeParts = parts[1].split(":");
            year = dateParts[0]; month = dateParts[1]; day = dateParts[2];
            hour = timeParts[0]; minute = timeParts[1]; second = timeParts[2];
        } else if (s.contains("-")) { // 2. 'yyyy-MM-dd'
            String[] dateParts = s.split("-");
            year = dateParts[0]; month = dateParts[1]; day = dateParts[2];
        } else if (s.length() == 8 && s.matches("\\d+")) { // 3. 'yyyyMMdd'
            year = s.substring(0, 4);
            month = s.substring(4, 6);
            day = s.substring(6, 8);
        } else if (s.matches("\\d{10}")) { // 4. Unix timestamp (sec)
            ZonedDateTime zdt = Instant.ofEpochSecond(Long.parseLong(s)).atZone(timeZone);
            java.time.LocalDateTime dt = zdt.toLocalDateTime();
            year = String.valueOf(dt.getYear());
            month = String.format("%02d", dt.getMonthValue());
            day = String.format("%02d", dt.getDayOfMonth());
            hour = String.format("%02d", dt.getHour());
            minute = String.format("%02d", dt.getMinute());
            second = String.format("%02d", dt.getSecond());
        } else {
            throw new IllegalArgumentException("Unsupported format : " + s);
        }

        // record는 생성 시점에 모든 걸 결정해야 함
        Date newDate = new Date(year, month, day);
        newDate.setTime(hour, minute, second); // setTime 메서드 호출
        return newDate;
    }

    public void setTime(String hour, String minute, String second) {
        this.hour = (hour.length() == 1) ? "0" + hour : hour;
        this.minute = (minute.length() == 1) ? "0" + minute : minute;
        this.second = (second.length() == 1) ? "0" + second : second;
    }

    /**
     *
     * @param type - Type of date string output.<br/>0 : returns `YYYY-MM-dd HH:mm:ss`<br/>1 : returns `YYYYMMdd`<br/>2 : returns `YYYY.MM.dd.`<br/>3 : returns Linux Timestamp (EpochSecond)
     */
    public String getDate(int type) {
        switch (type) {
            case 0 -> { return "%s-%s-%s %s:%s:%s".formatted(year, month, day, hour, minute, second); }
            case 1 -> { return "%s%s%s".formatted(year, month, day); }
            case 2 -> { return "%s.%s.%s.".formatted(year, month, day); }
            case 3 -> {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                LocalDateTime localDateTime = LocalDateTime.parse("%s%s%s%s%s%s".formatted(year, month, day, hour, minute, second), formatter);
                return Long.toString(
                        localDateTime.atZone(timeZone)
                                .toInstant()
                                .getEpochSecond()
                );
            }
            default -> { return null; }
        }
    }

    /**
     * @return `dkcal_mdays_YYMMdd` : it_unique_id
     */
    public String getUniqueId() {
        return "dkcal_mdays_" + getDate(1);
    }
}
