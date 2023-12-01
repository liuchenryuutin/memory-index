package org.lccy.lucene.memory.builder;

import org.apache.lucene.document.Document;
import org.lccy.lucene.memory.index.config.IndexConfig;
import org.lccy.lucene.memory.index.mapping.IndexFieldMapping;

import java.util.Map;

/**
 * Lucene文档构造builder
 *
 * @Date: 2024/01/17 15:12 <br>
 * @author: liuchen11
 */
public final class DocumentBuilder {

    private DocumentBuilder() {}

    /**
     * 构造文档
     * @param document
     * @param indexConfig
     * @return
     */
    public static Document build(Map<String, Object> document, IndexConfig indexConfig) {
        Document result = new Document();
        String primaryName = indexConfig.getPrimaryField().getName();
        if (!document.containsKey(primaryName)) {
            throw new IllegalArgumentException("MemoryIndex document must has primary field:" + primaryName);
        }
        for (Map.Entry<String, Object> entry : document.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            if(indexConfig.isSystemKeyword(fieldName)) {
                continue;
            }
            IndexFieldMapping fieldConf = indexConfig.getFieldConfig(fieldName);
            if(fieldConf.isDefaultFd() && !indexConfig.getIndexSetting().isDynamicsMapping()) {
                continue;
            }
            fieldConf.getType().convertField(result, fieldName, fieldConf, value);
        }
        return result;
    }
}
