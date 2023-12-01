package org.lccy.lucene.memory.aggs.leaf;

import org.apache.lucene.search.Scorable;

import java.io.IOException;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/12/11 15:12 <br>
 * @author: liuchen11
 */
public class LeafBucketCollectorBase extends LeafBucketCollector {


    private final LeafBucketCollector sub;

    /**
     * @param sub 子分类收集器
     */
    public LeafBucketCollectorBase(LeafBucketCollector sub) {
        this.sub = sub;
    }

    @Override
    public void setScorer(Scorable s) throws IOException {
        sub.setScorer(s);
    }

    @Override
    public void collect(int doc, long bucket) throws IOException {
        sub.collect(doc, bucket);
    }
}
