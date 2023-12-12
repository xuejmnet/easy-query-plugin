package com.easy.query.plugin.core.util;

import com.intellij.util.ArrayUtil;

/**
 * create time 2023/9/16 14:19
 * 文件说明
 *
 * @author xuejiaming
 */
public class StrUtil {
    public static boolean isBlank(CharSequence str) {
        int length;
        if (str != null && (length = str.length()) != 0) {
            for(int i = 0; i < length; ++i) {
                if (!CharUtil.isBlankChar(str.charAt(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    public static boolean isNotBlank(CharSequence str) {
        return !isBlank(str);
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(CharSequence str) {
        return !isEmpty(str);
    }
    public static String subAfter(CharSequence string, CharSequence separator, boolean isLastSeparator) {
        if (isEmpty(string)) {
            return null == string ? null : "";
        } else if (separator == null) {
            return "";
        } else {
            String str = string.toString();
            String sep = separator.toString();
            int pos = isLastSeparator ? str.lastIndexOf(sep) : str.indexOf(sep);
            return -1 != pos && string.length() - 1 != pos ? str.substring(pos + separator.length()) : "";
        }
    }

    public static int count(CharSequence content, CharSequence strForSearch) {
        if (!hasEmpty(content, strForSearch) && strForSearch.length() <= content.length()) {
            int count = 0;
            int idx = 0;
            String content2 = content.toString();

            for(String strForSearch2 = strForSearch.toString(); (idx = content2.indexOf(strForSearch2, idx)) > -1; idx += strForSearch.length()) {
                ++count;
            }

            return count;
        } else {
            return 0;
        }
    }
    public static boolean hasEmpty(CharSequence... strs) {
        if (ArrayUtil.isEmpty(strs)) {
            return true;
        } else {
            CharSequence[] var1 = strs;
            int var2 = strs.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                CharSequence str = var1[var3];
                if (isEmpty(str)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static String upperFirst(CharSequence str) {
        if (null == str) {
            return null;
        } else {
            if (str.length() > 0) {
                char firstChar = str.charAt(0);
                if (Character.isLowerCase(firstChar)) {
                    return Character.toUpperCase(firstChar) + subSuf(str, 1);
                }
            }

            return str.toString();
        }
    }
    public static String subSuf(CharSequence string, int fromIndex) {
        return isEmpty(string) ? null : sub(string, fromIndex, string.length());
    }


    public static String str(CharSequence cs) {
        return null == cs ? null : cs.toString();
    }
    public static String sub(CharSequence str, int fromIndexInclude, int toIndexExclude) {
        if (isEmpty(str)) {
            return str(str);
        } else {
            int len = str.length();
            if (fromIndexInclude < 0) {
                fromIndexInclude += len;
                if (fromIndexInclude < 0) {
                    fromIndexInclude = 0;
                }
            } else if (fromIndexInclude > len) {
                fromIndexInclude = len;
            }

            if (toIndexExclude < 0) {
                toIndexExclude += len;
                if (toIndexExclude < 0) {
                    toIndexExclude = len;
                }
            } else if (toIndexExclude > len) {
                toIndexExclude = len;
            }

            if (toIndexExclude < fromIndexInclude) {
                int tmp = fromIndexInclude;
                fromIndexInclude = toIndexExclude;
                toIndexExclude = tmp;
            }

            return fromIndexInclude == toIndexExclude ? "" : str.toString().substring(fromIndexInclude, toIndexExclude);
        }
    }

    public static String subBefore(CharSequence string, CharSequence separator, boolean isLastSeparator) {
        if (!isEmpty(string) && separator != null) {
            String str = string.toString();
            String sep = separator.toString();
            if (sep.isEmpty()) {
                return "";
            } else {
                int pos = isLastSeparator ? str.lastIndexOf(sep) : str.indexOf(sep);
                if (-1 == pos) {
                    return str;
                } else {
                    return 0 == pos ? "" : str.substring(0, pos);
                }
            }
        } else {
            return null == string ? null : string.toString();
        }
    }

    public static String subBefore(CharSequence string, char separator, boolean isLastSeparator) {
        if (isEmpty(string)) {
            return null == string ? null : "";
        } else {
            String str = string.toString();
            int pos = isLastSeparator ? str.lastIndexOf(separator) : str.indexOf(separator);
            if (-1 == pos) {
                return str;
            } else {
                return 0 == pos ? "" : str.substring(0, pos);
            }
        }
    }

    public static String toCamelCase(String str) {

        String[] splitArr = str.split("_");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < splitArr.length; i++) {
            if (i == 0) {
                sb.append(toLowerCaseFirstOne(splitArr[0]));
                continue;
            }

            sb.append(toUpperCaseFirstOne(splitArr[i]));
        }

        return sb.toString();
    }
    // 首字母转大写
    public static String toUpperCaseFirstOne(String s) {
        if (Character.isUpperCase(s.charAt(0))) {
            return s;
        } else {
            return Character.toUpperCase(s.charAt(0)) +
                    s.substring(1);
        }
    }
    // 首字母转小写
    public static String toLowerCaseFirstOne(String s) {
        if (Character.isLowerCase(s.charAt(0))) {
            return s;
        } else {
            return Character.toLowerCase(s.charAt(0)) +
                    s.substring(1);
        }
    }


}
