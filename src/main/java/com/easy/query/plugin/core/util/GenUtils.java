package com.easy.query.plugin.core.util;

import org.apache.commons.lang.WordUtils;

/**
 * create time 2023/12/13 08:47
 * 文件说明
 *
 * @author xuejiaming
 */
public class GenUtils {

    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String[] tablePrefixArray) {
        if (null != tablePrefixArray && tablePrefixArray.length > 0) {
            for (String tablePrefix : tablePrefixArray) {
                if (tableName.startsWith(tablePrefix)){
                    tableName = tableName.replaceFirst(tablePrefix, "");
                }
            }
        }
        return columnToJava(tableName);
    }
}
