package com.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Mor on 02/08/2016.
 */
public abstract class DateUtils {

    public static String getCurrDateTimeStr(String format) {
        return new SimpleDateFormat(format).format(new Date());
    }
}
