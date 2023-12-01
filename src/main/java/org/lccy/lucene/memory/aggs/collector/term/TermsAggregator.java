package org.lccy.lucene.memory.aggs.collector.term;

import org.lccy.lucene.memory.aggs.collector.Aggregator;
import org.lccy.lucene.memory.aggs.collector.BucketsAggregator;
import org.lccy.lucene.memory.aggs.sort.BucketsSort;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/12/11 20:34 <br>
 * @author: liuchen11
 */
public abstract class TermsAggregator extends BucketsAggregator {

    // term分桶字段
    protected String field;
    // term分桶返回的大小
    protected int size = -1;

    public TermsAggregator(String name, String field, Aggregator parent, BucketsSort bucketsSort) {
        super(name, parent, bucketsSort);
        this.field = field;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
