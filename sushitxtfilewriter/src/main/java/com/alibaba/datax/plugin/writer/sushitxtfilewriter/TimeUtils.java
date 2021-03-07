package com.alibaba.datax.plugin.writer.sushitxtfilewriter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author sushi
 * @create 2021-03-06 8:11 PM
 */
public class TimeUtils {
    public static String  getTimeStringFromTimestampMillis(long timestamp){
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        return format.format(new Date(timestamp));
    }
}
