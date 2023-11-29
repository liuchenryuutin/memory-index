package org.lccy.lucene.util;

import com.sun.istack.internal.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/11/29 11:34 <br>
 * @author: liuchen11
 */
public class CollectionUtils {

    public static boolean isEmpty(Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    /**
     * Return {@code true} if the supplied Map is {@code null} or empty.
     * Otherwise, return {@code false}.
     * @param map the Map to check
     * @return whether the given Map is empty
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return (map == null || map.isEmpty());
    }
}
