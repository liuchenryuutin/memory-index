package org.lccy.lucene.memory.aggs.collector;

import com.carrotsearch.hppc.LongLongHashMap;
import com.carrotsearch.hppc.LongLongMap;
import org.lccy.lucene.memory.aggs.collector.aggregation.InternalAggregation;
import org.lccy.lucene.memory.aggs.leaf.LeafBucketCollector;
import org.lccy.lucene.memory.aggs.sort.BucketsSort;
import org.lccy.lucene.memory.exception.QueryException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 多桶分组收集器的抽象类，只会记录桶数和每桶的大小，具体收集统计在子分组查询中。
 *
 * @Date: 2023/12/11 15:39 <br>
 * @author: liuchen11
 */
public abstract class BucketsAggregator extends AggregatorBase {

    //elasticsearch使用BigArrays进行结果收集，防止在数组容量不够的情况下频繁的扩容，提高性能，这里由于内存加载数据有限，不考虑
    private final LongLongMap bucketOrds = new LongLongHashMap();
    //桶排序器
    protected final BucketsSort bucketsSort;

    public BucketsAggregator(String name, Aggregator parent, BucketsSort bucketsSort) {
        super(name, parent);
        this.bucketsSort = bucketsSort;
    }

    /**
     * Utility method to collect the given doc in the given bucket (identified by the bucket ordinal)
     */
    public final void collectBucket(LeafBucketCollector subCollector, int doc, long bucketOrd) throws IOException {
        if (bucketOrd > Integer.MAX_VALUE) {
            throw new QueryException("bucket size is too large. please make it smaller");
        }
        collectExistingBucket(subCollector, doc, bucketOrd);
    }

    /**
     * Same as {@link #collectBucket(LeafBucketCollector, int, long)}, but doesn't check if the docCounts needs to be re-sized.
     */
    public final void collectExistingBucket(LeafBucketCollector subCollector, int doc, long bucketOrd) throws IOException {
        bucketOrds.addTo(bucketOrd, 1);
        subCollector.collect(doc, bucketOrd);
    }

    public final long bucketDocCount(long bucketOrd) {
        if (bucketOrds.containsKey(bucketOrd)) {
            return bucketOrds.get(bucketOrd);
        } else {
            return 0;
        }
    }

    /**
     * Required method to build the child aggregations of the given bucket (identified by the bucket ordinal).
     */
    protected final List<InternalAggregation> bucketAggregations(long bucket) throws IOException {
        final InternalAggregation[] aggregations = new InternalAggregation[subAggregators.size()];
        for (int i = 0; i < subAggregators.size(); i++) {
            aggregations[i] = subAggregators.get(i).buildAggregation(bucket);
        }
        return Arrays.asList(aggregations);
    }
}
