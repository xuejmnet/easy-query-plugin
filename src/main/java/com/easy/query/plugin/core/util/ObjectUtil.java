package com.easy.query.plugin.core.util;

import java.util.function.Supplier;

/**
 * create time 2023/9/16 14:19
 * 文件说明
 *
 * @author xuejiaming
 */
public class ObjectUtil {
    public static <T> T defaultIfNull(T object, T defaultValue) {
        return isNull(object) ? defaultValue : object;
    }

    public static <T> T defaultIfNull(T source, Supplier<? extends T> defaultValueSupplier) {
        return isNull(source) ? defaultValueSupplier.get() : source;
    }

    public static <T> T defaultIfNull(Object source, Supplier<? extends T> handle, T defaultValue) {
        return isNotNull(source) ? handle.get() : defaultValue;
    }
    public static <T> T defaultIfEmpty(String str, Supplier<? extends T> handle, T defaultValue) {
        return StrUtil.isNotEmpty(str) ? handle.get() : defaultValue;
    }

    public static <T extends CharSequence> T defaultIfEmpty(T str, T defaultValue) {
        return StrUtil.isEmpty(str) ? defaultValue : str;
    }

    public static <T extends CharSequence> T defaultIfEmpty(T str, Supplier<? extends T> defaultValueSupplier) {
        return StrUtil.isEmpty(str) ? defaultValueSupplier.get() : str;
    }
    public static boolean isNull(Object obj) {
        return null == obj;
    }

    public static boolean isNotNull(Object obj) {
        return !isNull(obj);
    }
}
