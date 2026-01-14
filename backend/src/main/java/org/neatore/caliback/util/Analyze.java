package org.neatore.caliback.util;

import org.neatore.caliback.CaliBack;
import org.neatore.caliback.object.Date;
import org.neatore.caliback.object.Schedule;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Analyze {
    public static List<Schedule> getSchedules(ResultSet rs) {
        final List<Schedule> result = new ArrayList<>();
        try {
            String it_unique_id = rs.getString("it_unique_id").replace("dkcal_mdays_", "");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate date = LocalDate.parse(it_unique_id, formatter);

            String content = rs.getString("it_content");
            Scanner scan = new Scanner(content);

            while (scan.hasNext()) {
                result.add(new Schedule(new Date(Integer.toString(date.getYear()), Integer.toString(date.getMonthValue()), Integer.toString(date.getDayOfMonth())), scan.nextLine()));
            }
        } catch (SQLException e) {
            CaliBack.LOGGER.error(e);
        }
        return result;
    }
}
