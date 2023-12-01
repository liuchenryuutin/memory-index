package org.lccy.lucene.memory.index.config;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import org.lccy.lucene.memory.index.mapping.IndexFieldMapping;
import org.lccy.lucene.memory.index.mapping.IndexSettingMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

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
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
        if(inputStream == null) {
            throw new IllegalArgumentException("MemoryIndex classpath config file is empty.");
        }
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
        IndexMapping indexDto = JSON.parseObject(text.toString(), IndexMapping.class);

        // 初始化索引信息
        super.init(indexDto.getSetting(), indexDto.getMapping());
    }

    @Getter
    @Setter
    public static class IndexMapping {
        private List<IndexFieldMapping> mapping;
        private IndexSettingMapping setting;
    }
}
