package org.lccy.lucene.memory.aggs.collector.metrics;

import org.lccy.lucene.memory.aggs.collector.aggregation.Aggregation;
import org.lccy.lucene.memory.search.SearchHits;

/**
 * 接口名称： <br>
 * 接口描述： <br>
 *
 * @Date: 2023/12/13 10:24 <br>
 * @author: liuchen11
 */
public interface TopHits extends Aggregation {

    /**
     * @return The top matching hits for the bucket
     */
    SearchHits getHits();
}
