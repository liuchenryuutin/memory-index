package org.lccy.lucene.memory.loader;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.lccy.lucene.memory.index.config.IndexConfig;
import org.lccy.lucene.memory.index.dto.IndexFieldDto;
import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 从类路径下加载索引数据
 *
 * @Date: 2023/11/21 16:48 <br>
 * @author: liuchen11
 */
public class ClassPathIndexDataLoader implements IndexDataLoader {

    private static final Logger logger = LoggerFactory.getLogger(ClassPathIndexDataLoader.class);

    private String path;

    public ClassPathIndexDataLoader(String path) {
        this.path = path;
    }

    @Override
    public List<Document> load(IndexConfig indexConfig) {
        InputStream inputStream = getClass().getResourceAsStream(path);
        List<String> dataList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                dataList.add(line);
            }
        } catch (IOException e) {
            logger.error("classpath load memory index data error:" + e.getMessage(), e);
            throw new IllegalArgumentException("classpath load memory index data error", e);
        }

        List<Document> result = new ArrayList<>();
        String primaryField = indexConfig.getPrimaryField().getName();
        for (String data : dataList) {
            JSONObject dataObj = JSON.parseObject(data);
            if (!dataObj.containsKey(primaryField)) {
                throw new IllegalArgumentException("data must has primary field:" + primaryField);
            }
            Document document = new Document();
            for (Map.Entry<String, Object> entry : dataObj.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                IndexFieldDto fieldConf = indexConfig.getFieldConfig(fieldName);
                fieldConf.getType().convertField(document, fieldName, fieldConf, value);
            }

            result.add(document);
        }

        return result;
    }
}
