package com.easy.query.plugin.core.util;

/**
 * create time 2023/9/16 14:24
 * 文件说明
 *
 * @author xuejiaming
 */
public class CharUtil {

    public static boolean isBlankChar(char c) {
        return isBlankChar((int)c);
    }

    public static boolean isBlankChar(int c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == 65279 || c == 8234 || c == 0;
    }
}
