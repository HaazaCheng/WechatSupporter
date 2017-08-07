package net.sklcc.wechatsupporter.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by Hazza on 2016/8/9.
 */
public class TimeUtil {
    private TimeUtil() {}



    /**
     * 将当前时间转换为指定格式
     * @param currentTimeMillis
     * @return
     */
    public static String convertMillsToDateString(long currentTimeMillis) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(currentTimeMillis);

        return formatter.format(date);
    }



    public static String convertDateToDateString(Date date) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return formatter.format(date);
    }

    /**
     * 解析String为Date类型
     * @param date
     * @return
     * @throws ParseException
     */
    public static Date convertStringToDate(String date) throws ParseException {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.parse(date);
    }

    /**
     * 得到两个日期相差的天数
     * @param fDate
     * @param oDate
     * @return
     */
    public static int getIntervalDays(Date fDate, Date oDate) {
        if (null == fDate || null == oDate) {
            return -1;
        }
        long intervalMilli = oDate.getTime() - fDate.getTime();

        return (int) (intervalMilli / (24 * 60 * 60 * 1000));
    }

    /**
     * 给某个时间增加指定的天数
     * @param date
     * @param i
     * @return
     */
    public static Date addDay(Date date, int i) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.add(calendar.DATE, i);
        return calendar.getTime();
    }

    /**
     * 获得今天是星期几，以大写英文字符串返回
     *
     * @param date 日期
     * @return　是星期几
     */
    public static String getDayOfWeek(Date date) {
        String[] weekDays = {"SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0) w = 0;
        return weekDays[w];
    }

    /**
     * 获得该日期是一个月的第几天
     *
     * @param date　日期
     * @return　第几天
     */
    public static int getDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        return d;
    }

    /**
     * 返回指定日期的上一个月
     *
     * @param date　指定日期
     * @return　返回一个int数组，数组第一个值表示年份，第二个值表示月份
     */
    public static int[] getLastMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int lastMonth = cal.get(Calendar.MONTH);
        if (lastMonth < 1) {
            lastMonth = 12;
            --year;
        }

        return new int[]{year, lastMonth};
    }

    /**
     * 按照给定的年份，月份，起始日，结束日，生成例如"xxxx-xx-xx"的模板日期，
     * 缺０会补齐０，并做相关非法性判断
     *
     * @param year　年份
     * @param month　月份
     * @param startDay　起始日期
     * @param endDay　结束日期
     * @return　包含模板日期的String数组
     */
    public static String[] generateFormatData(int year, int month, int startDay, int endDay) {
        if (month < 1 || month > 12) throw new IllegalArgumentException("The month is invalid!");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        int maxDays = cal.getActualMaximum(Calendar.DATE);
        if (startDay < 1) throw new IllegalArgumentException("The startDay is less than 1!");
        if (endDay > maxDays) throw new IllegalArgumentException("The startDay is less than the max days in month of " + month);

        String template = String.valueOf(year) + "-" +
                (month < 10 ? "0" + String.valueOf(month) : String.valueOf(month)) + "-";

        String[] res = new String[endDay - startDay + 1];
        for (int i = startDay; i <= endDay; i++) {
            if (i >= 1 && i <= 9) {
                res[i - startDay] = template + "0" + String.valueOf(i);
            } else {
                res[i - startDay] = template + String.valueOf(i);
            }
        }

        return res;
    }

    /**
     * 只给定年份和月份，生成该月份的所有模板日期
     *
     * @param year　年份
     * @param month　月份
     * @return　该月份的所有模板日期
     */
    public static String[] generateFormatData(int year, int month) {
        if (month < 1 || month > 12) throw new IllegalArgumentException("The month is invalid!");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        int maxDays = cal.getActualMaximum(Calendar.DATE);
        return generateFormatData(year, month, 1, maxDays);
    }

    /**
     * 获得明天的格式化的日期，以字符串形式，格式为"xxxx-xx-xx"
     *
     * @return 返回的格式化的日期字符串
     */
    public static String getTomorrowFormatDate() {
        Date d = TimeUtil.addDay(new Date(), 1);
        String[] strs = convertDateToDateString(d).split("\\s+");

        return strs[0].trim();
    }


    public static void main(String[] args) throws ParseException {

//        System.out.println(TimeUtil.convertMillsToDate(1470728691463L));
//        System.out.println(TimeUtil.convertStringToDate("2016-11-23"));
//        System.out.println(TimeUtil.getIntervalDays(TimeUtil.convertStringToDate("2016-11-22"), TimeUtil.convertStringToDate("2016-11-23")));
//        System.out.println(TimeUtil.addDay(TimeUtil.convertStringToDate("2016-11-12"), 1));
//        System.out.println(TimeUtil.convertStringToDate("2016-11-12"));
//        System.out.println(TimeUtil.getWeekOfDate(new Date()));
        /*String[] res = TimeUtil.generateFormatData(2017, 2);
        for (String s: res) {
            System.out.println(s);
        }*/

//        System.out.println(TimeUtil.getTomorrowFormatDate());
//        System.out.println(getDayOfMonth(new Date()));
        System.out.println(getLastMonth(new Date())[0] + " " + getLastMonth(new Date())[1]);
    }
}
