package org.lccy.lucene.memory.index.config;

import org.lccy.lucene.memory.constants.FieldTypeEnum;
import org.lccy.lucene.memory.index.dto.IndexFieldDto;
import org.lccy.lucene.memory.index.dto.IndexSettingDto;
import org.lccy.lucene.util.StringUtil;
import org.apache.lucene.analysis.Analyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 索引配置
 *
 * @Date: 2023/11/21 16:33 <br>
 * @author: liuchen11
 */
public class IndexConfig {

    private static final Logger logger = LoggerFactory.getLogger(IndexConfig.class);

    protected Map<String, IndexFieldDto> fieldConfigMap;
    protected IndexSettingDto indexSetting;
    protected IndexFieldDto primaryField;
    protected IndexFieldDto defaultField;

    public IndexConfig() {
        this.defaultField = new IndexFieldDto(null, false, FieldTypeEnum.STORE, null, null, null, false);
    }

    public IndexConfig(IndexSettingDto indexSetting, List<IndexFieldDto> fieldConfigs) {
        this();
        this.indexSetting = indexSetting;
        this.fieldConfigMap = new ConcurrentHashMap<>();
        for (IndexFieldDto field : fieldConfigs) {
            fieldConfigMap.put(field.getName(), field);
            if (field.isPrimary()) {
                this.primaryField = field;
            }
        }
        if (this.primaryField == null) {
            throw new IllegalArgumentException("MemoryIndex must has primary key");
        }
        if (FieldTypeEnum.KEYWORD != this.primaryField.getType()) {
            throw new IllegalArgumentException("The type of primary key must be keyword");
        }
    }

    /**
     * 获取已经设置的字段的分词器
     *
     * @return
     */
    public Map<String, Analyzer> getFieldAnalyzers() {
        Map<String, Analyzer> fieldAnalyzer = new HashMap<>();
        fieldConfigMap.forEach((fieldName, conf) -> {
            if (StringUtil.isNotEmpty(conf.getAnalyzer())) {
                fieldAnalyzer.put(fieldName, conf.convertAnalyzer());
            }
        });
        return fieldAnalyzer;
    }

    /**
     * 根据字段名获取字段配置
     *
     * @param fieldName
     * @return
     */
    public IndexFieldDto getFieldConfig(String fieldName) {
        IndexFieldDto result = fieldConfigMap.get(fieldName);
        if (result == null) {
            result = defaultField;
        }
        return result;
    }

    public boolean containsField(String fieldName) {
        return this.fieldConfigMap.containsKey(fieldName);
    }

    /**
     * 获取主键配置
     *
     * @return
     */
    public IndexFieldDto getPrimaryField() {
        return primaryField;
    }

    /**
     * 获取索引设置
     *
     * @return
     */
    public IndexSettingDto getIndexSetting() {
        return indexSetting;
    }
}
