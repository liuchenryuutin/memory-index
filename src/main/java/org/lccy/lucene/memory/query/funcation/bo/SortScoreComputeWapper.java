package org.lccy.lucene.memory.query.funcation.bo;

import org.lccy.lucene.memory.constants.Constants;
import org.lccy.lucene.memory.exception.QueryException;
import org.lccy.lucene.memory.query.funcation.ComplexFieldFunction;
import org.lccy.lucene.memory.util.CommonUtil;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * wrap sort score query param <br>
 *
 * @author liuchen <br>
 * @date 2023-07-11
 */
public class SortScoreComputeWapper {

    public static final String WEIGHT = "weight";
    public static final String FIELD = "field";
    public static final String TYPE = "type";
    public static final String VALUE = "value";

    private Map<String, Object> sortScore;
    private String field;
    private String type;
    private String value;
    private Integer weight;

    public SortScoreComputeWapper(Map<String, Object> st) {
        Integer weight = st.get(WEIGHT) == null ? null : Integer.parseInt(st.get(WEIGHT).toString());
        String field = CommonUtil.toString(st.get(FIELD));
        String type = CommonUtil.toString(st.get(TYPE));
        String value = CommonUtil.toString(st.get(VALUE));
        if (weight == null) {
            throwsException(ComplexFieldFunction.NAME + " query param [categorys] [sort_score] must has [weight], please check.");
        }
        if (CommonUtil.isEmpty(field) && !Constants.ComplexFieldFunction.SortValueType.ANY.equals(type)) {
            throwsException(ComplexFieldFunction.NAME + " query param [categorys] [sort_score], When the [type] is not [any], [field] must be set.");
        }
        if (CommonUtil.isEmpty(value) && !(Constants.ComplexFieldFunction.SortValueType.ANY.equals(type)
                || Constants.ComplexFieldFunction.SortValueType.EXISTS.equals(type)
                || Constants.ComplexFieldFunction.SortValueType.NOT_EXISTS.equals(type))) {
            throwsException(ComplexFieldFunction.NAME + " query param [categorys] [sort_score], When the [type] is not [any, exists, not_exists], [value] must be set.");
        }

        this.sortScore = st;
        this.field = field;
        this.type = type;
        this.value = value;
        this.weight = weight;
    }

    public Integer getWeight() {
        return weight;
    }

    public String getField() {
        return field;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getExpression(double sortBaseSocre) {
        String oper = Constants.ComplexFieldFunction.SortValueType.NOT.equals(this.getType()) ? "!=" : "=";
        return String.format(Locale.ROOT, "if %s %s %s, then exec %s * %f, else do nothing.", getField(), oper, getValue(), getWeight().toString(), sortBaseSocre);
    }

    /**
     * 数值是否满足匹配条件
     * @param values
     * @return
     */
    public boolean match(String[] values) {
        return matchNew(this.getType(), this.getValue(), values);
    }

    /**
     * 数值是否满足匹配条件
     * @param type
     * @param expectVal
     * @param values
     * @return
     */
    public static boolean matchNew(String type, String expectVal, String[] values) {
        if (CommonUtil.isEmpty(type)) {
            type = Constants.ComplexFieldFunction.SortValueType.EQUAL;
        }

        switch (type) {
            case Constants.ComplexFieldFunction.SortValueType.EXISTS:
                return values != null;
            case Constants.ComplexFieldFunction.SortValueType.NOT_EXISTS:
                return values == null;
            case Constants.ComplexFieldFunction.SortValueType.IN:
                if (values == null || values.length == 0 || expectVal == null) {
                    return false;
                }
                for (String val : values) {
                    if (val == null) {
                        continue;
                    }
                    if (expectVal.indexOf(val) >= 0) {
                        return true;
                    }
                }
                return false;
            case Constants.ComplexFieldFunction.SortValueType.NOT_IN:
                if (expectVal == null) {
                    return false;
                }
                if (values == null || values.length == 0) {
                    return true;
                }
                for (String val : values) {
                    if (val == null) {
                        continue;
                    }
                    if (expectVal.indexOf(val) >= 0) {
                        return false;
                    }
                }
                return true;
            case Constants.ComplexFieldFunction.SortValueType.NOT:
                if (expectVal == null) {
                    return false;
                }
                if (values == null || values.length == 0) {
                    return true;
                }
                for (String val : values) {
                    if (expectVal.equals(val)) {
                        return false;
                    }
                }
                return true;
            case Constants.ComplexFieldFunction.SortValueType.EQUAL:
            default:
                if (values == null || values.length == 0 || expectVal == null) {
                    return false;
                }
                for (String val : values) {
                    if (expectVal.equals(val)) {
                        return true;
                    }
                }
                return false;
        }
    }

    private void throwsException(String msg) {
        throw new QueryException(msg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortScoreComputeWapper that = (SortScoreComputeWapper) o;
        return Objects.equals(sortScore, that.sortScore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sortScore);
    }
}
