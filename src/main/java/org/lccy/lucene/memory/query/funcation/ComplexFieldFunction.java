package org.lccy.lucene.memory.query.funcation;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Explanation;
import org.lccy.lucene.memory.constants.Constants;
import org.lccy.lucene.memory.exception.LuceneException;
import org.lccy.lucene.memory.index.config.IndexConfig;
import org.lccy.lucene.memory.index.mapping.IndexFieldMapping;
import org.lccy.lucene.memory.query.funcation.bo.CategoryScoreWapper;
import org.lccy.lucene.memory.query.funcation.bo.FieldScoreComputeWapper;
import org.lccy.lucene.memory.query.funcation.bo.SortScoreComputeWapper;
import org.lccy.lucene.memory.util.CommonUtil;
import org.lccy.lucene.memory.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 自定义评分-复合查询评分计算类
 *
 * @Date: 2023/12/02 21:39 <br>
 * @author: liuchen11
 */
public class ComplexFieldFunction implements ScoreFunction {

    public static final String NAME = "ComplexFieldFunction";

    private final CategoryScoreWapper csw;
    private final IndexConfig indexConfig;

    public ComplexFieldFunction(CategoryScoreWapper categorys, IndexConfig indexConfig) {
        this.csw = categorys;
        this.indexConfig = indexConfig;
    }

    @Override
    public double score(LeafReaderContext context, int docId, float subQueryScore) throws LuceneException, IOException {

        Document document = context.reader().document(docId);
        String categoryName = csw.getCategoryField();
        String categoryCode = getStrValue(document.getField(categoryName), indexConfig.getFieldConfig(categoryName));
        if (CommonUtil.isEmpty(categoryCode)) {
            return csw.getOriginalScoreFactor() * subQueryScore;
        }

        List<FieldScoreComputeWapper> fieldScores = csw.getFieldScoreWappers(categoryCode);
        List<SortScoreComputeWapper> sortScores = csw.getScoreComputeWappers(categoryCode);

        double fieldScoreTotal = 0;
        if (!CommonUtil.isEmpty(fieldScores)) {
            String fieldMode = csw.getFieldMode();
            for (FieldScoreComputeWapper fbo : fieldScores) {
                IndexFieldMapping fieldMapping  = indexConfig.getFieldConfig(fbo.getField());
                if(!fieldMapping.canComplexFunctionScore()) {
                    throw new LuceneException("field:" + fbo.getField() + " not support complex funcation sort, filed are not numerical.");
                }

                Object fVal = fieldMapping.convertStoreValue(document.getField(fbo.getField()));
                if (fVal == null) {
                    if (!fbo.getRequire()) {
                        continue;
                    } else if (fbo.getRequire() && CommonUtil.isEmpty(fbo.getMissing())) {
                        throw new IllegalArgumentException("require field " + fbo.getField() + "must has a value or has a missing value");
                    } else {
                        fVal = fieldMapping.convertScoreMissingValue(fbo.getMissing());
                    }
                }
                if(fVal == null) {
                    continue;
                }
                fieldScoreTotal = mergeFieldScore(fieldMode, fieldScoreTotal, fbo.computeScore(fVal));
            }
        }

        double sortScoreTotal = 0;
        if (!CommonUtil.isEmpty(sortScores)) {
            double sortBaseScore = csw.getSortBaseScore();
            for (SortScoreComputeWapper sbo : sortScores) {
                if(Constants.ComplexFieldFunction.SortValueType.ANY.equals(sbo.getType())) {
                    sortScoreTotal = mergeSortScore(Constants.ComplexFieldFunction.SortMode.MAX, sortScoreTotal, sbo.getWeight() * sortBaseScore);
                    break;
                }

                String field = sbo.getField();
                boolean match = false;
                if(field.indexOf(Constants.ComplexFieldFunction.SPLIT) > 0) {
                    // 多个字段时，按照Constants.SPLIT后处理
                    String[] fields = field.split(Constants.ComplexFieldFunction.SPLIT);
                    String[] types = sbo.getType().split(Constants.ComplexFieldFunction.SPLIT);
                    String[] values = sbo.getValue().split(Constants.ComplexFieldFunction.SPLIT);
                    for(int i = 0; i < fields.length; i++) {
                        String f = fields[i];
                        String type = types.length > i ? types[i] : null;
                        String value = values.length > i ? values[i] : null;
                        IndexFieldMapping fieldMapping  = indexConfig.getFieldConfig(f);
                        if(!fieldMapping.canComplexFunctionSort()) {
                            throw new LuceneException("field:" + f + " not support complex funcation rating sorting.");
                        }

                        IndexableField[] indexFields = document.getFields(f);
                        String[] vals = getStrValues(indexFields, fieldMapping);
                        // 字段是否匹配
                        match = SortScoreComputeWapper.matchNew(type, value, vals);
                        if(!match) {
                            break;
                        }
                    }
                } else {
                    IndexFieldMapping fieldMapping  = indexConfig.getFieldConfig(field);
                    if(!fieldMapping.canComplexFunctionSort()) {
                        throw new LuceneException("field:" + field + " not support complex funcation sort.");
                    }
                    IndexableField[] indexFields = document.getFields(field);
                    String[] fVal = getStrValues(indexFields, fieldMapping);
                    match = sbo.match(fVal);
                }
                if (match) {
                    sortScoreTotal = mergeSortScore(Constants.ComplexFieldFunction.SortMode.MAX, sortScoreTotal, sbo.getWeight() * sortBaseScore);
                    break;
                }
            }
        }

        return csw.getFuncScoreFactor() * fieldScoreTotal + csw.getOriginalScoreFactor() * subQueryScore + sortScoreTotal;
    }

    /**
     * 合并多个字段值计算的评分
     * @param fieldMode
     * @param total
     * @param target
     * @return
     */
    public double mergeFieldScore(String fieldMode, double total, double target) {
        if (target < 0) {
            return total;
        }
        switch (fieldMode) {
            case Constants.ComplexFieldFunction.FieldMode.SUM:
                total += target;
                break;
            case Constants.ComplexFieldFunction.FieldMode.MULT:
                total *= target;
                break;
            case Constants.ComplexFieldFunction.SortMode.MAX:
                total = total < target ? target : total;
                break;
            case Constants.ComplexFieldFunction.SortMode.MIN:
                total = total > target ? target : total;
                break;
            default:
                total += target;
        }
        return total;
    }

    /**
     * 合并多个字段值排序的评分
     * @param sortMode
     * @param total
     * @param target
     * @return
     */
    public double mergeSortScore(String sortMode, double total, double target) {
        if (target < 0) {
            return total;
        }
        switch (sortMode) {
            case Constants.ComplexFieldFunction.SortMode.MAX:
                total = total < target ? target : total;
                break;
            case Constants.ComplexFieldFunction.SortMode.MIN:
                total = total > target ? target : total;
                break;
            default:
                total = total < target ? target : total;
        }
        return total;
    }

    /**
     * 获取字段值
     * @param indexFields
     * @param fieldMapping
     * @return
     */
    private String[] getStrValues(IndexableField[] indexFields, IndexFieldMapping fieldMapping) {
        String[] values = null;
        if(indexFields.length > 0) {
            values = new String[indexFields.length];
            int idx = 0;
            for(IndexableField indexableField : indexFields) {
                values[idx++] = StringUtil.conver2String(fieldMapping.convertStoreValue(indexableField));
            }
        }
        return values;
    }

    private String getStrValue(IndexableField indexField, IndexFieldMapping fieldMapping) {
        String value = null;
        if(indexField != null) {
            value = StringUtil.conver2String(fieldMapping.convertStoreValue(indexField));
        }
        return value;
    }

    @Override
    public Explanation explain(LeafReaderContext context, int doc, Explanation subQueryScore) throws LuceneException, IOException {

        Document document = context.reader().document(doc);
        String categoryName = csw.getCategoryField();
        String categoryCode = getStrValue(document.getField(categoryName), indexConfig.getFieldConfig(categoryName));
        if (CommonUtil.isEmpty(categoryCode)) {
            return Explanation.match(csw.getOriginalScoreFactor() * subQueryScore.getValue().floatValue()
                    , String.format("category is empty. subQueryScore:[%f], expression:[%f * subScore]", subQueryScore.getValue().floatValue(), csw.getOriginalScoreFactor()));
        }
        List<FieldScoreComputeWapper> fieldScores = csw.getFieldScoreWappers(categoryCode);
        List<SortScoreComputeWapper> sortScores = csw.getScoreComputeWappers(categoryCode);

        double fieldScoreTotal = 0;
        Explanation fieldsExplain = null;
        if (!CommonUtil.isEmpty(fieldScores)) {
            String fieldMode = csw.getFieldMode();
            List<Explanation> fieldExplanList = new ArrayList<>();
            for (FieldScoreComputeWapper fbo : fieldScores) {

                boolean useMissing = false;
                IndexFieldMapping fieldMapping  = indexConfig.getFieldConfig(fbo.getField());
                if(!fieldMapping.canComplexFunctionScore()) {
                    throw new LuceneException("field:" + fbo.getField() + " not support complex funcation sort, filed are not numerical.");
                }
                Object fVal = fieldMapping.convertStoreValue(document.getField(fbo.getField()));
                if (fVal == null) {
                    if (!fbo.getRequire()) {
                        continue;
                    } else if (fbo.getRequire() && CommonUtil.isEmpty(fbo.getMissing())) {
                        throw new IllegalArgumentException("require field " + fbo.getField() + "must has a value or has a missing value");
                    } else {
                        fVal = fieldMapping.convertScoreMissingValue(fbo.getMissing());
                        useMissing = true;
                    }
                }
                if(fVal == null) {
                    continue;
                }
                double fieldScore = fbo.computeScore(fVal);
                fieldScoreTotal = mergeFieldScore(fieldMode, fieldScoreTotal, fieldScore);

                Explanation fex = Explanation.match(fieldScore, String.format(Locale.ROOT, "Compute field:[%s], using missing:[%s], expression:[%s].",
                        fbo.getField(), useMissing, fbo.getExpression(fVal)));
                fieldExplanList.add(fex);
            }

            fieldsExplain = Explanation.match(fieldScoreTotal, String.format(Locale.ROOT, "Compute fieldScoreTotal, filed_mode:[%s].", fieldMode), fieldExplanList);
        }

        double sortScoreTotal = 0;
        Explanation sortExplain = null;
        if (!CommonUtil.isEmpty(sortScores)) {
            double sortBaseScore = csw.getSortBaseScore();
            List<Explanation> sortExplanList = new ArrayList<>();
            for (SortScoreComputeWapper sbo : sortScores) {
                if(Constants.ComplexFieldFunction.SortValueType.ANY.equals(sbo.getType())) {
                    double sortScore = sbo.getWeight() * sortBaseScore;
                    sortScoreTotal = mergeSortScore(Constants.ComplexFieldFunction.SortMode.MAX, sortScoreTotal, sortScore);
                    Explanation sortEx = Explanation.match(sortScore, "Compute sort type:[any], expression:[it's always true].");
                    sortExplanList.add(sortEx);
                    break;
                }

                String field = sbo.getField();
                boolean match = false;
                StringBuilder fVals = new StringBuilder();
                if(field.indexOf(Constants.ComplexFieldFunction.SPLIT) > 0) {
                    // 多个字段时，按照Constants.SPLIT后处理
                    String[] fields = field.split(Constants.ComplexFieldFunction.SPLIT);
                    String[] types = sbo.getType().split(Constants.ComplexFieldFunction.SPLIT);
                    String[] values = sbo.getValue().split(Constants.ComplexFieldFunction.SPLIT);
                    for(int i = 0; i < fields.length; i++) {
                        String f = fields[i];
                        String type = types.length > i ? types[i] : null;
                        String value = values.length > i ? values[i] : null;

                        IndexFieldMapping fieldMapping  = indexConfig.getFieldConfig(f);
                        if(!fieldMapping.canComplexFunctionSort()) {
                            throw new LuceneException("field:" + f + " not support complex funcation rating sorting.");
                        }

                        IndexableField[] indexFields = document.getFields(f);
                        String[] vals = getStrValues(indexFields, fieldMapping);

                        if(fVals.length() != 0) {
                            fVals.append(Constants.ComplexFieldFunction.SPLIT);
                        }
                        fVals.append(Arrays.toString(vals));
                        match = SortScoreComputeWapper.matchNew(type, value, vals);
                        if(!match) {
                            break;
                        }
                    }
                } else {
                    IndexFieldMapping fieldMapping  = indexConfig.getFieldConfig(field);
                    if(!fieldMapping.canComplexFunctionSort()) {
                        throw new LuceneException("field:" + field + " not support complex funcation sort.");
                    }

                    IndexableField[] indexFields = document.getFields(field);
                    String[] fVal = getStrValues(indexFields, fieldMapping);
                    fVals.append(Arrays.toString(fVal));
                    match = sbo.match(fVal);
                }

                if (match) {
                    double sortScore = sbo.getWeight() * sortBaseScore;
                    sortScoreTotal = mergeSortScore(Constants.ComplexFieldFunction.SortMode.MAX, sortScoreTotal, sbo.getWeight() * sortBaseScore);
                    Explanation sortEx = Explanation.match(sortScore, String.format(Locale.ROOT, "Compute sort field:[%s], value:[%s], expression:[%s].",
                            sbo.getField(), fVals, sbo.getExpression(sortBaseScore)));
                    sortExplanList.add(sortEx);
                    break;
                }
            }
            sortExplain = Explanation.match(sortScoreTotal, String.format(Locale.ROOT, "Compute sortScoreTotal, sort_mode:[max], sort_base_score:[%f] ", sortBaseScore), sortExplanList);
        }

        float subScore = subQueryScore.getValue().floatValue();
        double score = csw.getFuncScoreFactor() * fieldScoreTotal + csw.getOriginalScoreFactor() * subScore + sortScoreTotal;
        List<Explanation> resList = new ArrayList<>();
        if (fieldsExplain != null) {
            resList.add(fieldsExplain);
        }
        if (sortExplain != null) {
            resList.add(sortExplain);
        }
        Explanation result = Explanation.match(
                (float) score,
                String.format(Locale.ROOT,
                        "Compute complex_field_score, subScore:[%f] expression: [%f * fieldScoreTotal + %f * subScore + sortScoreTotal]",
                        subScore, csw.getFuncScoreFactor(), csw.getOriginalScoreFactor()), resList);
        return result;
    }

}
