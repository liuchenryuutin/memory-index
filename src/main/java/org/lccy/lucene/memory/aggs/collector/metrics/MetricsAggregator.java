package org.lccy.lucene.memory.aggs.collector.metrics;

import org.lccy.lucene.memory.aggs.collector.Aggregator;
import org.lccy.lucene.memory.aggs.collector.AggregatorBase;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/12/11 15:03 <br>
 * @author: liuchen11
 */
public abstract class MetricsAggregator extends AggregatorBase {

    protected static final Aggregator[] SUB_EMPTY = {};

    public MetricsAggregator(String name, Aggregator parent) {
        super(name, parent);
    }
}
