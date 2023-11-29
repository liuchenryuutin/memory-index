package org.lccy.lucene.memory.loader;

import org.lccy.lucene.memory.index.config.IndexConfig;
import org.apache.lucene.document.Document;

import java.util.List;

/**
 * 索引数据加载接口
 *
 * @Date: 2023/11/21 16:20 <br>
 * @author: liuchen11
 */
public interface IndexDataLoader {

    /**
     * 加载数据
     *
     * @return
     */
    List<Document> load(IndexConfig indexConfig);
}
