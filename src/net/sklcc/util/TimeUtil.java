package net.sklcc.util;

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
        Calendar calendar   =   new GregorianCalendar();
        calendar.setTime(date);
        calendar.add(calendar.DATE, i);
        return calendar.getTime();
    }

    /**
     * 获得今天是星期几
     * @param date
     * @return
     */
    public static String getWeekOfDate(Date date) {
        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0)
            w = 0;
        return weekDays[w];
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



    public static void main(String[] args) throws ParseException {

//        System.out.println(TimeUtil.convertMillsToDate(1470728691463L));
//        System.out.println(TimeUtil.convertStringToDate("2016-11-23"));
//        System.out.println(TimeUtil.getIntervalDays(TimeUtil.convertStringToDate("2016-11-22"), TimeUtil.convertStringToDate("2016-11-23")));
//        System.out.println(TimeUtil.addDay(TimeUtil.convertStringToDate("2016-11-12"), 1));
//        System.out.println(TimeUtil.convertStringToDate("2016-11-12"));
//        System.out.println(TimeUtil.getWeekOfDate(new Date()));
        String[] res = TimeUtil.generateFormatData(2017, 2);
        for (String s: res) {
            System.out.println(s);
        }

    }
}
