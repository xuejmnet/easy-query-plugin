package com.easy.query.plugin.core;

import com.intellij.openapi.application.ApplicationInfo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * create time 2024/11/23 16:05
 * 文件说明
 *
 * @author xuejiaming
 */
public class VersionUtil {
    private static final LocalDateTime AFTER_2024_3_TIME = LocalDateTime.of(2024, 11, 1, 0, 0);
    public static boolean isAfter2024_3(){

        try {

            Calendar buildDate = ApplicationInfo.getInstance().getBuildDate();
            Date time = buildDate.getTime();
            LocalDateTime buildTime = LocalDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault());
            return buildTime.isAfter(AFTER_2024_3_TIME);
        }catch (Exception ignored){

        }
        return false;
    }
}
