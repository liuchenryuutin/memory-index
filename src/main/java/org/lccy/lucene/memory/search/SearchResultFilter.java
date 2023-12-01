package org.lccy.lucene.memory.search;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;

/**
 * 查询结果过滤器
 *
 * @Date: 2023/12/12 14:53 <br>
 * @author: liuchen11
 */
public interface SearchResultFilter {

    /**
     * 过滤结果集
     * @param scoreDoc 查询的doc信息
     * @param document 当前文档
     * @return true:跳过当前文档  false:匹配当前文档
     */
    boolean filter(ScoreDoc scoreDoc, Document document);
}
