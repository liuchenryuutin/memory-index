package org.lccy.lucene.memory.query.funcation.bo;

import org.lccy.lucene.memory.constants.Constants;
import org.lccy.lucene.memory.exception.QueryException;
import org.lccy.lucene.memory.query.funcation.ComplexFieldFunction;
import org.lccy.lucene.memory.util.CommonUtil;

import java.util.*;

/**
 * wrap category query param <br>
 *
 * @author liuchen <br>
 * @date 2023-07-11
 */
public class CategoryScoreWapper {

    public static final String CATEGORY_FIELD = "category_field";
    public static final String FUNC_SCORE_FACTOR = "func_score_factor";
    public static final String ORIGINAL_SCORE_FACTOR = "original_score_factor";
    public static final String FIELD_MODE = "field_mode";
    public static final String FIELDS_SCORE = "fields_score";
    public static final String SORT_BASE_SCORE = "sort_base_score";
    public static final String SORT_SCORE = "sort_score";
    // wrap data
    private Map<String, Object> categorys;

    private Double funcScoreFactor;
    private Double originalScoreFactor;
    private String categoryField;
    private String fieldMode;
    private Double sortBaseScore;

    private Map<String, List<FieldScoreComputeWapper>> fieldScoreWapperMap;
    private Map<String, List<SortScoreComputeWapper>> scoreComputeWapperMap;
    private Map<String, Boolean> allFiled;

    public CategoryScoreWapper(Map<String, Object> categorys) {
        if (categorys.get(FUNC_SCORE_FACTOR) == null) {
            throwsException(ComplexFieldFunction.NAME + " query must has field [func_score_factor]");
        } else {
            funcScoreFactor = Double.parseDouble(categorys.get(FUNC_SCORE_FACTOR).toString());
        }
        if (categorys.get(ORIGINAL_SCORE_FACTOR) == null) {
            throwsException(ComplexFieldFunction.NAME + " query must has field [original_score_factor]");
        } else {
            originalScoreFactor = Double.parseDouble(categorys.get(ORIGINAL_SCORE_FACTOR).toString());
        }
        if (funcScoreFactor < 0 || originalScoreFactor < 0) {
            throwsException(ComplexFieldFunction.NAME + " query param [original_score_factor] or [func_score_factor] must be greater than 0.");
        }
        if (categorys.get(CATEGORY_FIELD) == null) {
            throwsException(ComplexFieldFunction.NAME + " query must has field [category_field]");
        } else {
            categoryField = CommonUtil.toString(categorys.get(CATEGORY_FIELD));
        }

        String fieldMode = CommonUtil.toString(categorys.get(FIELD_MODE));
        Map<String, Object> fieldsScore = (Map<String, Object>) categorys.get(FIELDS_SCORE);
        Double sortBaseScore = categorys.get(SORT_BASE_SCORE) == null ? null : Double.parseDouble(categorys.get(SORT_BASE_SCORE).toString());
        Map<String, Object> sortScore = (Map<String, Object>) categorys.get(SORT_SCORE);
        if (CommonUtil.isEmpty(fieldsScore) && CommonUtil.isEmpty(sortScore)) {
            throwsException(ComplexFieldFunction.NAME + " query must has [name] and [fields_score] or [sort_score], please check.");
        }
        if (!CommonUtil.isEmpty(fieldsScore) && CommonUtil.isEmpty(fieldMode)) {
            throwsException(ComplexFieldFunction.NAME + " query param [fields_score] must has sibling element [field_mode], please check.");
        }
        if (!CommonUtil.isEmpty(sortScore) && sortBaseScore == null) {
            throwsException(ComplexFieldFunction.NAME + " query param [sort_score] must has sibling element [sort_base_score], please check.");
        }
        this.fieldMode = fieldMode;
        this.sortBaseScore = sortBaseScore;

        this.allFiled = new HashMap<>();
        this.allFiled.put(categoryField, true);
        if (!CommonUtil.isEmpty(fieldsScore)) {
            this.fieldScoreWapperMap = new HashMap<>();
            for (String cateCodes : fieldsScore.keySet()) {
                List<Map> value = (List<Map>) fieldsScore.get(cateCodes);
                if (CommonUtil.isEmpty(value)) {
                    throwsException(ComplexFieldFunction.NAME + " query param [fields_score] must has attributes, please check.");
                }
                List<FieldScoreComputeWapper> fieldScoreComputeWappers = new ArrayList<>();
                value.stream().forEach(x -> {
                    FieldScoreComputeWapper fscw = new FieldScoreComputeWapper(x);
                    fieldScoreComputeWappers.add(fscw);
                    String field = fscw.getField();
                    boolean require = fscw.getRequire() && fscw.getMissing() == null;
                    // 多个字段
                    if (field.indexOf(Constants.ComplexFieldFunction.SPLIT) > 0) {
                        String[] fields = field.split(Constants.ComplexFieldFunction.SPLIT);
                        for (String f : fields) {
                            this.allFiled.put(f, require);
                        }
                    } else {
                        this.allFiled.put(field, require);
                    }

                });

                for (String cateCode : cateCodes.split(",", -1)) {
                    this.fieldScoreWapperMap.put(cateCode, fieldScoreComputeWappers);
                }
            }
        }

        if (!CommonUtil.isEmpty(sortScore)) {
            this.scoreComputeWapperMap = new HashMap<>();
            for (String cateCodes : sortScore.keySet()) {
                List<Map> value = (List<Map>) sortScore.get(cateCodes);
                if (CommonUtil.isEmpty(value)) {
                    throwsException(ComplexFieldFunction.NAME + " query param [sort_score] must has attributes, please check.");
                }
                List<SortScoreComputeWapper> scoreComputeWappers = new ArrayList<>();
                value.stream().forEach(x -> {
                    SortScoreComputeWapper sscw = new SortScoreComputeWapper(x);
                    scoreComputeWappers.add(sscw);
                    String field = sscw.getField();
                    // 多个字段
                    if (field.indexOf(Constants.ComplexFieldFunction.SPLIT) > 0) {
                        String[] fields = field.split(Constants.ComplexFieldFunction.SPLIT);
                        for (String f : fields) {
                            this.allFiled.put(f, false);
                        }
                    } else {
                        this.allFiled.put(field, false);
                    }
                });

                if (!CommonUtil.isEmpty(scoreComputeWappers)) {
                    scoreComputeWappers.sort(Comparator.comparingInt(SortScoreComputeWapper::getWeight).reversed());
                }

                for (String cateCode : cateCodes.split(",", -1)) {
                    this.scoreComputeWapperMap.put(cateCode, scoreComputeWappers);
                }
            }
        }

        this.categorys = categorys;
    }

    public Double getFuncScoreFactor() {
        return funcScoreFactor;
    }

    public Double getOriginalScoreFactor() {
        return originalScoreFactor;
    }

    public String getCategoryField() {
        return categoryField;
    }

    public String getFieldMode() {
        return fieldMode;
    }

    public Double getSortBaseScore() {
        return sortBaseScore;
    }

    public Map<String, Object> unwrap() {
        return categorys;
    }

    public List<FieldScoreComputeWapper> getFieldScoreWappers(String cateCode) {
        return fieldScoreWapperMap == null ? null : fieldScoreWapperMap.get(cateCode);
    }

    public List<SortScoreComputeWapper> getScoreComputeWappers(String cateCode) {
        return scoreComputeWapperMap == null ? null : scoreComputeWapperMap.get(cateCode);
    }

    public Map<String, Object> getCategorys() {
        return categorys;
    }

    public Map<String, List<FieldScoreComputeWapper>> getFieldScoreWapperMap() {
        return fieldScoreWapperMap;
    }

    public Map<String, List<SortScoreComputeWapper>> getScoreComputeWapperMap() {
        return scoreComputeWapperMap;
    }

    public Map<String, Boolean> getAllFiled() {
        return allFiled;
    }

    private void throwsException(String msg) {
        throw new QueryException(msg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryScoreWapper that = (CategoryScoreWapper) o;
        return Objects.equals(categorys, that.categorys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categorys);
    }
}
