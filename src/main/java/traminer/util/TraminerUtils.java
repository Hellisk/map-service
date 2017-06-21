package traminer.util;

import com.sun.javafx.util.Utils;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Implementation of miscellaneous utilities functions.
 *
 * @author uqdalves
 */
@SuppressWarnings({"serial", "restriction"})
public final class TraminerUtils implements Serializable {

    /**
     * Returns a double value with a positive sign, greater than or
     * equal to 0.0 and less than the given number.
     */
    public static double random(double number) {
        return number * Math.random();
    }

    /**
     * The mean of the values in this list.
     */
    public static double getMean(List<Double> list) {
        int size = list.size();
        double sum = 0.0;
        for (double value : list) {
            sum += value;
        }
        return (sum / size);
    }

    /**
     * The standard deviation of values in this list.
     */
    public static double getStd(List<Double> list, double mean) {
        double size = list.size();
        double dif_sum2 = 0.0;
        for (double value : list) {
            dif_sum2 += (value - mean) * (value - mean);
        }
        return Math.sqrt(dif_sum2 / size);
    }

    /**
     * <p>
     * Format example: "EEE MMM d HH:mm:ss zzz yyyy"
     *
     * @param date
     * @param format
     */
    public static Date parseDate(String inputDate, String format) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern(format, Locale.ENGLISH);
        ZonedDateTime dateTime = formatter.parse(inputDate, ZonedDateTime::from);
        Date date = Date.from(dateTime.toInstant());

        return date;
    }

    public static boolean isUnix() {
        return Utils.isUnix();
    }

    public static boolean isWindows() {
        return Utils.isWindows();
    }

    public static boolean isMac() {
        return Utils.isMac();
    }
}
