package org.lccy.lucene.memory.util;

import java.util.regex.Pattern;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/04/21 17:15 <br>
 * @author: liuchen11
 */
public final class StringUtil {

    private StringUtil() {
    }

    private static final Pattern pattern = Pattern.compile("[\\p{Punct}\\pP\\s]+");

    public static boolean isEmpty(String str) {
        if (null == str) {
            return true;
        } else {
            return "".equals(str.trim());
        }
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isPunct(String str) {
        return isEmpty(str) ? false : pattern.matcher(str).matches();
    }

    public static String conver2String(Object obj) {
        if(obj == null) {
            return null;
        }
        if(obj instanceof String) {
            return (String) obj;
        } else {
            return obj.toString();
        }
    }
}
