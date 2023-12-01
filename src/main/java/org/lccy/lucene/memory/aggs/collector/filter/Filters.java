package org.lccy.lucene.memory.aggs.collector.filter;

import org.lccy.lucene.memory.aggs.collector.aggregation.MultiBucketsAggregation;

import java.util.List;

/**
 * 接口名称： <br>
 * 接口描述： <br>
 *
 * @Date: 2023/12/11 19:19 <br>
 * @author: liuchen11
 */
public interface Filters extends MultiBucketsAggregation {
    /**
     * A bucket associated with a specific filter (identified by its key)
     */
    interface Bucket extends MultiBucketsAggregation.Bucket {
    }

    /**
     * The buckets created by this aggregation.
     */
    @Override
    List<? extends Bucket> getBuckets();

    Bucket getBucketByKey(String key);
}
