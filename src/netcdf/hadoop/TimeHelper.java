package netcdf.hadoop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.hadoop.util.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import ucar.unidata.util.StringUtil;

public class TimeHelper {

    public static String formatTime(DateTime time) {
        DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC();
        return formatter.print(time.getMillis());
    }

    public static String[] formatTimes(DateTime... times) {
        if (times == null)
            return null;
        ArrayList<String> strings = new ArrayList<String>();
        for (DateTime time: times)
            strings.add(formatTime(time));
        return strings.toArray(new String[0]);
    }

    public static String join(DateTime[][] timestamps) {
        ArrayList<String> array = new ArrayList<String>();
        for (DateTime[] nested: timestamps) {
            array.add(StringUtils.join("#", Arrays.asList(formatTimes(nested))));
        }
        return StringUtils.join(",", array);
    }

    public static DateTime[][] split(String string) {
        ArrayList<DateTime[]> array = new ArrayList<DateTime[]>();
        for (String substring: StringUtil.split(string)) {
            array.add(parseTimes(StringUtils.split(substring, '\\', '#')));
        }
        return array.toArray(new DateTime[0][]);
    }

    public static DateTime toDateTime(Date date) {
        return new DateTime(date.getTime());
    }

    public static DateTime toDateTime(String time) {
        DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis();
        return formatter.parseDateTime(time);
    }

    public static DateTime[] toDateTimes(Date... dates) {
        if (dates == null) return null;
        List<DateTime> times = new ArrayList<DateTime>();
        for (int n = 0; n < dates.length; ++n)
            times.add(toDateTime(dates[n]));
        return times.toArray(new DateTime[0]);
    }

    public static DateTime[] parseTimes(String... strings) {
        if (strings == null) return null;
        List<DateTime> times = new ArrayList<DateTime>();
        for (int n = 0; n < strings.length; ++n)
            times.add(toDateTime(strings[n]));
        return times.toArray(new DateTime[0]);
    }

}
