package org.neatore.caliback.util;

import org.neatore.caliback.CaliBack;
import org.neatore.caliback.object.Day;
import org.neatore.caliback.object.Date;
import org.neatore.caliback.object.Schedule;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DataUtils {
    /**
     * Returns list of Schedules from ResultSet for a single day
     * @param rs - Result Set for single day
     * @return - List of Schedules
     */
    public static List<Schedule> getSchedules(ResultSet rs) {
        final List<Schedule> result = new ArrayList<>();
        try {
            if (rs.next()) {
                String it_unique_id = rs.getString("it_unique_id").replace("dkcal_mdays_", "");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate date = LocalDate.parse(it_unique_id, formatter);

                String content = rs.getString("it_content");
                Scanner scan = new Scanner(content);

                while (scan.hasNext()) {
                    result.add(new Schedule(new Date(Integer.toString(date.getYear()), Integer.toString(date.getMonthValue()), Integer.toString(date.getDayOfMonth())), scan.nextLine()));
                }
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
        return result;
    }

    /**
     * Returns list of Days from ResultSet
     * @param rs - Result Set for multiple days
     * @return - List of Days
     */
    public static List<Day> getDays(ResultSet rs) {
        final List<Day> result = new ArrayList<>();
        try {
            while (rs.next()) {
                String it_unique_id = rs.getString("it_unique_id").replace("dkcal_mdays_", "");
                Date date = Date.parseDate(it_unique_id);

                String content = rs.getString("it_content");
                try (Scanner scan = new Scanner(content)) {
                    List<Schedule> schedules = new ArrayList<>();
                    while (scan.hasNext()) {
                        schedules.add(new Schedule(date, scan.nextLine()));
                    }
                    result.add(new Day(date, schedules, Date.parseDate(rs.getString("it_mdate")), rs.getString("it_bgColor"), rs.getString("it_history")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
        return result;
    }

    /**
     * Returns data map of target date.<br/>
     * @see java.util.Map
     * @param it_unique_id query id
     * @return data map of target date
     */
    public static Map<String, Object> getRecordData(String it_unique_id) {
        try (Connection conn = DriverManager.getConnection(CaliBack.dbc.dburl());
             PreparedStatement pstmt = conn.prepareStatement("SELECT * from item_table WHERE it_unique_id = ?")
        ) {
            pstmt.setString(1, it_unique_id);
            try (ResultSet rs = pstmt.executeQuery()) {
                Map<String, Object> result = new HashMap<>();
                if (rs.next()) {
                    result.put("u_mid", rs.getString("u_mid"));
                    result.put("it_unique_id", rs.getString("it_unique_id"));
                    result.put("it_content", rs.getString("it_content"));
                    result.put("it_history", rs.getString("it_history"));
                    result.put("it_cdate", rs.getString("it_cdate"));
                    result.put("it_mdate", rs.getString("it_mdate"));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
