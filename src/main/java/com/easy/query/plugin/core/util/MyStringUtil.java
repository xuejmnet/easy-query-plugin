package com.easy.query.plugin.core.util;

/**
 * create time 2024/1/31 17:18
 * 文件说明
 *
 * @author xuejiaming
 */
public class MyStringUtil {
    public static String lambdaShortName(String str,int index,int total) {
        char[] chars = str.toCharArray();
        if(chars.length==0){
            return "t";
        }
        for (int i = 0; i < chars.length; i++) {
            if (Character.isUpperCase(chars[i])) {
                String parameter = String.valueOf(chars[i]).toLowerCase();
                if(total>1){
                    return parameter+(index+1);
                }
                return parameter;
            }
        }
        return str.toLowerCase();
    }
}
