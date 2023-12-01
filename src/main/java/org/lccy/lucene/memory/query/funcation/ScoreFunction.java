package org.lccy.lucene.memory.query.funcation;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Explanation;
import org.lccy.lucene.memory.exception.LuceneException;

import java.io.IOException;

/**
 * 自定义评分查询计算接口
 *
 * @Date: 2023/12/02 20:23 <br>
 * @author: liuchen11
 */
public interface ScoreFunction {

    double score(LeafReaderContext context, int docId, float subQueryScore) throws LuceneException, IOException;

    Explanation explain(LeafReaderContext context, int doc, Explanation subQueryScore) throws LuceneException, IOException;
}
