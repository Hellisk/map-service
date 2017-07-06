package traminer.util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class DateParser {
    /**
     * <p>
     * Format example: "EEE MMM d HH:mm:ss zzz yyyy"
     *
     * @param inputDate
     * @param format
     */
    public static Date parseDate(String inputDate, String format) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern(format, Locale.ENGLISH);
        ZonedDateTime dateTime = formatter.parse(inputDate, ZonedDateTime::from);
        Date date = Date.from(dateTime.toInstant());

        return date;
    }
}
