package com.easy.query.plugin.core.util;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * create time 2024/1/31 17:18
 * 文件说明
 *
 * @author xuejiaming
 */
public class MyStringUtil {

    public static @NotNull String[] safeSplit(String value, String separatorChars) {
        return safeSplit(value, separatorChars, false);
    }

    public static @NotNull String[] safeSplit(String value, String separatorChars, boolean removeBank) {
        if (StrUtil.isBlank(value)) {
            return new String[0];
        }
        String[] split = value.split(separatorChars);
        if (removeBank) {
            ArrayList<String> result = new ArrayList<>(split.length);
            for (int i = 0; i < split.length; i++) {
                if (StringUtils.isNotBlank(split[i])) {
                    result.add(split[i]);
                }
            }
            return result.toArray(new String[0]);
        }
        return split;
    }
    public static @NotNull List<String> safeSplitList(String value, String separatorChars, boolean removeBank) {
        String[] strings = safeSplit(value, separatorChars, removeBank);
        return new ArrayList<>(Arrays.asList(strings));
    }
    /**
     * 将对象类型转成lambda入参短名称
     * @param str Topic || SysUser
     * @param index 在第几个参数位
     * @param total 总共有几个参数
     * @return
     */
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
    }/**
     * 转下划线字符, eg AaaBbb => AAA_BBB
     */
    public static String toUpperUnderlined(String s) {
        char[] chars = s.toCharArray();
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            if (i!=0&&Character.isUpperCase(chars[i])) {
                temp.append("_");
            }
            temp.append(Character.toUpperCase(chars[i]));
        }
        return temp.toString();
    }
    public static <T> String nullToDefault(T val, Function<T, String> create, String def) {
        if (val == null) {
            return def;
        }
        return create.apply(val);
    }
}
