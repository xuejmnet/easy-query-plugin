package com.easy.query.plugin.core.util;

/**
 * create time 2024/1/31 17:18
 * 文件说明
 *
 * @author xuejiaming
 */
public class MyStringUtil {
    public static String lambdaShortName(String str) {
        char[] chars = str.toCharArray();
        if(chars.length==0){
            return "t";
        }
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            if (Character.isUpperCase(chars[i])) {
                temp.append(chars[i]);
            }
        }
        String val = temp.toString().toLowerCase();
        if(val.length()==1){
            return str.toLowerCase();
        }
        return val;
    }
}
