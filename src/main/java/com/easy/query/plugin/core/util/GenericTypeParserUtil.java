package com.easy.query.plugin.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * create time 2025/8/6 09:51
 * 文件说明
 *
 * @author xuejiaming
 */
public class GenericTypeParserUtil {

    /**
     * 解析泛型类型字符串
     * @param fullType 完整的泛型类型字符串
     * @return 解析结果对象
     */
    public static ParsedType parseGenericType(String fullType) {
        ParsedType result = new ParsedType();

        // 提取最外层类型名和参数部分
        Matcher outerMatcher = Pattern.compile("([^<]+)<(.+)>").matcher(fullType);
        if (!outerMatcher.find()) {
            result.rawType = fullType;
            return result;
        }

        result.rawType = outerMatcher.group(1);
        String paramsStr = outerMatcher.group(2);

        // 解析类型参数
        result.typeArguments = splitGenericParameters(paramsStr);

        // 解析数字后缀
        Matcher numMatcher = Pattern.compile(".*?(\\d+)$").matcher(result.rawType);
        if (numMatcher.find()) {
            result.typeParameterCount = Integer.parseInt(numMatcher.group(1));
        }

        return result;
    }

    /**
     * 分割泛型参数（处理嵌套泛型）
     * @param paramsStr 参数字符串
     * @return 分割后的参数列表
     */
    private static List<String> splitGenericParameters(String paramsStr) {
        List<String> params = new ArrayList<>();
        int bracketCount = 0;
        StringBuilder current = new StringBuilder();

        for (char c : paramsStr.toCharArray()) {
            if (c == '<') {
                bracketCount++;
            } else if (c == '>') {
                bracketCount--;
            }

            if (c == ',' && bracketCount == 0) {
                params.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            params.add(current.toString().trim());
        }

        return params;
    }

    /**
     * 解析结果容器类
     */
    public static class ParsedType {
        public String rawType;                   // 原始类型名
        public int typeParameterCount = -1;      // 类型参数数量
        public List<String> typeArguments;       // 类型参数列表

        /**
         * 解析嵌套的泛型参数
         * @param index 参数索引
         * @return 嵌套参数的解析结果
         */
        public ParsedType parseNestedType(int index) {
            if (typeArguments == null || index >= typeArguments.size()) {
                return null;
            }
            return parseGenericType(typeArguments.get(index));
        }

        @Override
        public String toString() {
            return "RawType: " + rawType +
                "\nTypeParamCount: " + typeParameterCount +
                "\nArguments: " + typeArguments;
        }
    }
}
