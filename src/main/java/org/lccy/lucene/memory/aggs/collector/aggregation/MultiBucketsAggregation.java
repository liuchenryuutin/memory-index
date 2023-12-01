package org.lccy.lucene.memory.aggs.collector.aggregation;

import java.util.List;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/12/11 19:20 <br>
 * @author: liuchen11
 */
public interface MultiBucketsAggregation extends Aggregation {
    /**
     * A bucket represents a criteria to which all documents that fall in it adhere to. It is also uniquely identified
     * by a key, and can potentially hold sub-aggregations computed over all documents in it.
     */
    interface Bucket {
        /**
         * @return The key associated with the bucket
         */
        Object getKey();

        /**
         * @return The key associated with the bucket as a string
         */
        String getKeyAsString();

        /**
         * @return The number of documents that fall within this bucket
         */
        long getDocCount();

        /**
         * @return  The sub-aggregations of this bucket
         */
        List<? extends Aggregation> getAggregations();

        /**
         * Return the bucket max_score
         */
        float getMaxScore();
    }

    /**
     * @return  The buckets of this aggregation.
     */
    List<? extends Bucket> getBuckets();
}
