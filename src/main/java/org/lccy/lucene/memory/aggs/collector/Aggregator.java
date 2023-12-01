package org.lccy.lucene.memory.aggs.collector;

import org.lccy.lucene.memory.aggs.collector.aggregation.InternalAggregation;

import java.io.IOException;
import java.util.List;

/**
 * 所有分组实现参考ElasticSearch，部分代码直接从ElasticSearch中clone，简化了部分逻辑<br/>
 * ps: ElasticSearch的代码抽象逻辑还是很清晰的，大多数分组没有实现，待空闲时实现。
 *
 * @Date: 2023/12/11 19:10 <br>
 * @author: liuchen11
 */
public abstract class Aggregator extends BucketCollector {

    private boolean isCollected;
    /**
     * Return the name of this aggregator.
     */
    public abstract String name();

    /**
     * Return the parent aggregator.
     */
    public abstract Aggregator parent();

    /**
     * Return the sub aggregator with the provided name.
     */
    public abstract Aggregator subAggregator(String name);

    /**
     * Return all sub aggregator
     */
    public abstract List<Aggregator> subAggregator();

    /**
     * Build an aggregation for data that has been collected into {@code bucket}.
     */
    public InternalAggregation buildAggregation(long bucket) throws IOException {
        isCollected = true;
        return doBuildAggregation(bucket);
    }

    protected abstract InternalAggregation doBuildAggregation(long bucket) throws IOException;

    /**
     * Build an empty aggregation.
     */
    public InternalAggregation buildEmptyAggregation() {
        isCollected = true;
        return doBuildEmptyAggregation();
    }

    /**
     * Build an empty aggregation.
     */
    public abstract InternalAggregation doBuildEmptyAggregation();


    /**
     * 是否已经收集
     */
    public boolean isCollected() {
        return isCollected;
    }
}
