package org.lccy.lucene.memory.index.dto;

import com.alibaba.fastjson.JSON;
import org.lccy.lucene.memory.analyzer.AnalyzerRepository;
import org.lccy.lucene.memory.constants.FieldTypeEnum;
import org.lccy.lucene.util.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexableField;

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
public class IndexFieldDto {

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
    // 是否构造docValue
    private boolean docValue;

    public Analyzer convertAnalyzer() {
        return AnalyzerRepository.getAnalyzer(this.analyzer);
    }

    public Analyzer convertSearchAnalyzer() {
        String analyzerStr = StringUtil.isNotEmpty(this.searchAnalyzer) ? this.searchAnalyzer : this.analyzer;
        return AnalyzerRepository.getAnalyzer(analyzerStr);
    }

    /**
     * 根据字段类型提取查询结果
     * @param field
     * @return
     */
    public Object convertStoreValue(IndexableField field) {
        Object result;
        switch (type) {
            case KEYWORD:
            case TEXT:
            case DATE:
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
            default:
                result = field.stringValue();
        }
        return result;
    }
}
