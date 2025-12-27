package org.neatore.caliback.object;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public record Date(String year, String month, String day) {
    // ** DEFUALT ZONE ID **
    private static final ZoneId timeZone = ZoneId.of("Asia/Seoul");

    public static final class Now {
        public static Date toDate() {
            Instant instant = Instant.now();
            ZonedDateTime zdt = instant.atZone(timeZone);
            return new Date(Integer.toString(zdt.getYear()), Integer.toString(zdt.getMonthValue()), Integer.toString(zdt.getDayOfMonth()));
        }

        public static String getUnixTime() {
            Instant instant = Instant.now();
            return Long.toString(instant.atZone(timeZone).toEpochSecond());
        }

        public static String format(String format) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            return Instant.ofEpochSecond(Long.parseLong(getUnixTime()))
                    .atZone(timeZone)
                    .format(formatter);
        }
    }

    private static String hour, minute, second;
    public void setTime(String hour, String minute, String second) {
        Date.hour = (hour.length() == 1) ? "0" + hour : hour;
        Date.minute = (minute.length() == 1) ? "0" + minute : minute;
        Date.second = (second.length() == 1) ? "0" + second : second;
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
