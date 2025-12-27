package org.neatore.calisync.util;

import org.neatore.calisync.CaliSync;
import org.neatore.calisync.object.Date;
import org.neatore.calisync.object.Schedule;

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
                // TODO: 취소선으로 isCompleted 판단하기
                result.add(new Schedule(new Date(Integer.toString(date.getYear()), Integer.toString(date.getMonthValue()), Integer.toString(date.getDayOfMonth())), scan.nextLine(), false));
            }
        } catch (SQLException e) {
            CaliSync.LOGGER.error(e);
        }
        return result;
    }
}
