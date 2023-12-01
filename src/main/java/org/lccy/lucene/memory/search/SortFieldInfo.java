package org.lccy.lucene.memory.search;

import lombok.Getter;
import org.lccy.lucene.memory.constants.Constants;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/11/30 10:59 <br>
 * @author: liuchen11
 */
@Getter
public class SortFieldInfo {

    public enum SortMode {
        ASC,
        DESC
    }

    // 评分排序
    public static final SortFieldInfo _SCORE = new SortFieldInfo(Constants._SCORE);
    // id排序（含义不大）
    public static final SortFieldInfo _ID = new SortFieldInfo(Constants._ID);

    // 排序字段名
    private String name;
    // 排序自定值（距离排序时，排序的参考点）
    private Object value;
    // 缺省值
    private Object missingValue;
    // 排序规则
    private SortMode sortMode;

    public SortFieldInfo() {
    }

    public SortFieldInfo(String name) {
        this(name, null);
    }

    public SortFieldInfo(String name, Object value) {
        this(name, value ,null);
    }

    public SortFieldInfo(String name, Object value, Object missingValue) {
        this(name, value, missingValue, SortMode.DESC);
    }

    public SortFieldInfo(String name, Object value, Object missingValue, SortMode sortMode) {
        this.name = name;
        this.missingValue = missingValue;
        this.value = value;
        this.sortMode = sortMode;
    }
}
