package org.lccy.lucene.memory.aggs.collector.metrics;

import org.lccy.lucene.memory.aggs.collector.aggregation.InternalAggregation;
import org.lccy.lucene.memory.search.SearchHits;

/**
 * Top aggs的结果实现类
 *
 * @Date: 2023/12/13 10:27 <br>
 * @author: liuchen11
 */
public class InternalTopHits extends InternalAggregation implements TopHits {

    private final SearchHits hits;
    private final float maxScore;

    public InternalTopHits(String name, SearchHits hits, float maxScore) {
        super(name);
        this.hits = hits;
        this.maxScore = maxScore;
    }

    @Override
    protected float doGetMaxScore() {
        return maxScore;
    }

    @Override
    public SearchHits getHits() {
        return hits;
    }
}
