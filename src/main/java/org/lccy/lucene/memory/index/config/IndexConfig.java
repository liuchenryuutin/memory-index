package org.lccy.lucene.memory.index.config;

import org.apache.lucene.analysis.Analyzer;
import org.lccy.lucene.memory.constants.Constants;
import org.lccy.lucene.memory.constants.FieldTypeEnum;
import org.lccy.lucene.memory.index.mapping.IndexFieldMapping;
import org.lccy.lucene.memory.index.mapping.IndexSettingMapping;
import org.lccy.lucene.memory.util.StringUtil;
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

    protected Map<String, IndexFieldMapping> fieldConfigMap;
    protected IndexSettingMapping indexSetting;
    protected IndexFieldMapping primaryField;
    protected IndexFieldMapping defaultField;

    public IndexConfig() {
        this.defaultField = new IndexFieldMapping(null, false, FieldTypeEnum.STORE, null, null, null, false, true, true);
    }

    /**
     * 初始化索引信息
     * @param indexSetting 索引设置
     * @param fieldMappings 字段映射
     */
    public void init(IndexSettingMapping indexSetting, List<IndexFieldMapping> fieldMappings) {
        this.indexSetting = indexSetting;
        this.fieldConfigMap = new ConcurrentHashMap<>();
        for (IndexFieldMapping field : fieldMappings) {
            if(FieldTypeEnum.KEYWORD == field.getType()) {
                field.setDocValue(true);
            }
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
    public IndexFieldMapping getFieldConfig(String fieldName) {
        IndexFieldMapping result = fieldConfigMap.get(fieldName);
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
    public IndexFieldMapping getPrimaryField() {
        return primaryField;
    }

    /**
     * 获取索引设置
     *
     * @return
     */
    public IndexSettingMapping getIndexSetting() {
        return indexSetting;
    }

    /**
     * 是否是系统预留字段
     * @param fieldName
     * @return
     */
    public boolean isSystemKeyword(String fieldName) {
        return Constants._ID.equals(fieldName) || Constants._SCORE.equals(fieldName);
    }
}
