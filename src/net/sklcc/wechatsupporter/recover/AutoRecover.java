package net.sklcc.wechatsupporter.recover;

import net.sklcc.wechatsupporter.util.TimeUtil;

/**
 * Created by hazza on 8/7/17.
 */
public class AutoRecover {
    private final String[] dates;

    /**
     * 构造函数
     *
     * @param year　年份
     * @param month　月份
     */
    public AutoRecover(int year, int month) {
        dates = TimeUtil.generateFormatData(year, month);
    }


}
