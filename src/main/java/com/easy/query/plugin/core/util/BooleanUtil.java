package com.easy.query.plugin.core.util;

/**
 * create time 2023/9/16 14:23
 * 文件说明
 *
 * @author xuejiaming
 */
public class BooleanUtil {

    public static boolean isTrue(Boolean bool) {
        return Boolean.TRUE.equals(bool);
    }

    public static boolean isFalse(Boolean bool) {
        return Boolean.FALSE.equals(bool);
    }
}
