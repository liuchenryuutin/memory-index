package org.lccy.lucene.memory.aggs.collector.term;

import org.lccy.lucene.memory.aggs.collector.aggregation.MultiBucketsAggregation;

import java.util.List;

/**
 * 接口名称： <br>
 * 接口描述： <br>
 *
 * @Date: 2023/12/11 20:00 <br>
 * @author: liuchen11
 */
public interface Terms extends MultiBucketsAggregation {


    /**
     * A bucket that is associated with a single term
     */
    interface Bucket extends MultiBucketsAggregation.Bucket {

    }

    /**
     * Return the sorted list of the buckets in this terms aggregation.
     */
    @Override
    List<? extends Bucket> getBuckets();

    /**
     * Get the bucket for the given term, or null if there is no such bucket.
     */
    Bucket getBucketByKey(String term);

    /**
     * 返回未在分桶内的文档个数
     */
    long getOtherDocCounts();

    /**
     * 获取分桶个数
     */
    long getBucketCounts();
}
