package com.crystalneko.tonekocommon.util;

public class StringUtil {

    // 获取字符串中含有某个特定字符串的个数
    public static int getCount(String str, String subStr) {
        if (str == null || subStr == null || str.isEmpty() || subStr.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(subStr, index)) != -1) {
            count++;
            index += subStr.length();
        }
        return count;
    }
}
