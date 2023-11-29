package org.lccy.lucene.memory.index.config;

import com.alibaba.fastjson.JSON;
import org.lccy.lucene.memory.constants.FieldTypeEnum;
import org.lccy.lucene.memory.index.dto.IndexDto;
import org.lccy.lucene.memory.index.dto.IndexFieldDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从类路径下加载索引配置
 *
 * @Date: 2023/11/21 16:44 <br>
 * @author: liuchen11
 */
public class ClassPathIndexConfig extends IndexConfig {

    private static final Logger logger = LoggerFactory.getLogger(ClassPathIndexConfig.class);

    public ClassPathIndexConfig(String path) {
        super();
        InputStream inputStream = getClass().getResourceAsStream(path);
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 处理每一行的内容
                text.append(line);
            }
        } catch (IOException e) {
            logger.error("classpath load memory index config error:{}", e.getMessage(), e);
            throw new IllegalArgumentException("MemoryIndex classpath load config error", e);
        }
        IndexDto indexDto = JSON.parseObject(text.toString(), IndexDto.class);
        super.indexSetting = indexDto.getSetting();
        this.fieldConfigMap = new ConcurrentHashMap<>();
        for (IndexFieldDto field : indexDto.getMapping()) {
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
}
