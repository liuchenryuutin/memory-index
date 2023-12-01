package org.lccy.lucene.memory.util;

import java.util.Collection;
import java.util.Map;

/**
 * StringUtil
 *
 * @author liuchen <br>
 * @date 2023-07-08
 */
public final class CommonUtil {

    private CommonUtil() {
    }

    public static boolean isEmpty(String str) {
        if (null == str) {
            return true;
        } else {
            return "".equals(str.trim());
        }
    }

    public static boolean isEmpty(Collection<?> list) {
        return list == null || list.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> list) {
        return list == null || list.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> list) {
        return !isEmpty(list);
    }

    public static boolean isNotEmpty(Map<?, ?> list) {
        return !isEmpty(list);
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static String toString(Object obj) {
        return obj == null ? "" : obj.toString();
    }
}
