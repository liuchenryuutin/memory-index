package org.lccy.lucene.memory.index.mapping;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.SortField;
import org.lccy.lucene.memory.analyzer.AnalyzerRepository;
import org.lccy.lucene.memory.constants.FieldTypeEnum;
import org.lccy.lucene.memory.exception.LuceneException;
import org.lccy.lucene.memory.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/11/21 16:53 <br>
 * @author: liuchen11
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IndexFieldMapping {

    // 字段名
    private String name;
    // 知否主键
    private boolean primary = false;
    // 字段类型
    private FieldTypeEnum type;
    // 分词器
    private String analyzer;
    // 搜索分词器
    private String searchAnalyzer;
    // 日期格式，type=date时需要指定
    private String format;
    // 是否构造docValue（默认关闭）
    private boolean docValue = false;
    // 是否是缺省字段
    private boolean defaultFd = false;
    // 是否保存字段（默认保存）
    private boolean store = true;

    /**
     * 获取字段存储分词器
     * @return
     */
    public Analyzer convertAnalyzer() {
        return AnalyzerRepository.getAnalyzer(this.analyzer);
    }

    /**
     * 获取字段搜索分词器
     * @return
     */
    public Analyzer convertSearchAnalyzer() {
        String analyzerStr = StringUtil.isNotEmpty(this.searchAnalyzer) ? this.searchAnalyzer : this.analyzer;
        return AnalyzerRepository.getAnalyzer(analyzerStr);
    }

    /**
     * 根据字段类型提取转换查询结果中的字段值
     * @param field
     * @return
     */
    public Object convertStoreValue(IndexableField field) {
        if(field == null) {
            return null;
        }
        Object result;
        switch (type) {
            case KEYWORD:
            case TEXT:
            case DATE:
            case STORE:
                result = field.stringValue();
                break;
            case LONG:
            case FLOAT:
            case DOUBLE:
                result = field.numericValue();
                break;
            case JSON:
                String str = field.stringValue();
                if(str.startsWith("[")) {
                    result = JSON.parseArray(str);
                } else {
                    result = JSON.parseObject(str);
                }
                break;
            case GEO_POINT:
                List<Double> latLon = new ArrayList<>();
                String point = field.stringValue();
                String[] vals = point.split(",", -1);
                Double lat = Double.parseDouble(vals[0].trim());
                Double lon = Double.parseDouble(vals[1].trim());
                latLon.add(lat);
                latLon.add(lon);
                result = latLon;
                break;
            default:
                throw new LuceneException("memory index not support type:" + type.getName());
        }
        return result;
    }

    /**
     * 字段是否支持排序
     * @return
     */
    public boolean canSort() {
        boolean result = false;
        switch (type) {
            case DATE:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case GEO_POINT:
                result = true;
                break;
            case KEYWORD:
                if(this.docValue) {
                    result = true;
                }
                break;
        }
        return result;
    }

    /**
     * 获取排序时字段类型
     * @return
     */
    public SortField.Type getSortType() {
        SortField.Type result = null;
        switch (type) {
            case DATE:
            case LONG:
                result = SortField.Type.LONG;
                break;
            case FLOAT:
                result = SortField.Type.FLOAT;
                break;
            case DOUBLE:
                result = SortField.Type.DOUBLE;
                break;
            case KEYWORD:
                result = SortField.Type.STRING;
                break;
        }
        return result;
    }

    /**
     * 判断排序字段缺失时的默认值是否合法
     * @param missingValue
     * @return
     */
    public boolean checkMissing(Object missingValue) {
        if(missingValue == null) {
            return true;
        }
        boolean result = false;
        switch (type) {
            case DATE:
            case LONG:
                result = missingValue instanceof Long;
                break;
            case FLOAT:
                result = missingValue instanceof Float;
                break;
            case DOUBLE:
                result = missingValue instanceof Double;
                break;
            case KEYWORD:
                result = missingValue instanceof String;
                break;
        }
        return result;
    }

    /**
     * 是否支持支持复合函数字段值评分
     * @return
     */
    public boolean canComplexFunctionScore() {
        boolean result = false;
        switch (type) {
            case LONG:
            case FLOAT:
            case DOUBLE:
            case GEO_POINT:
                result = true;
        }
        return result;
    }

    /**
     * 复合函数字段缺省值
     * @return
     */
    public Object convertScoreMissingValue(String missing) {
        if(missing == null) {
            return null;
        }
        Object result = null;
        switch (type) {
            case LONG:
            case FLOAT:
            case DOUBLE:
                result = Double.parseDouble(missing);
                break;
            case GEO_POINT:
                List<Double> geopoint = new ArrayList<>();
                String[] point = missing.split(",");
                geopoint.add(Double.parseDouble(point[0].trim()));
                geopoint.add(Double.parseDouble(point[1].trim()));
                result = true;
        }
        return result;
    }

    /**
     * 是否支持支持复合函数排序
     * @return
     */
    public boolean canComplexFunctionSort() {
        boolean result = false;
        switch (type) {
            case KEYWORD:
            case DOUBLE:
            case LONG:
            case FLOAT:
                result = true;
        }
        return result;
    }
}
