package org.lccy.lucene.memory.aggs.collector.filter;

import org.lccy.lucene.memory.aggs.collector.aggregation.InternalAggregation;
import org.lccy.lucene.memory.util.CommonUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * filter aggs分组结果实现类
 *
 * @Date: 2023/12/11 19:31 <br>
 * @author: liuchen11
 */
public class InternalFilters extends InternalAggregation implements Filters {

    private final List<InternalBucket> buckets;
    private transient Map<String, InternalBucket> bucketMap;

    public InternalFilters(String name, List<InternalBucket> buckets) {
        super(name);
        this.buckets = buckets;
    }

    @Override
    public List<? extends Bucket> getBuckets() {
        return buckets;
    }

    @Override
    public Bucket getBucketByKey(String key) {
        if (bucketMap == null) {
            bucketMap = new HashMap<>(buckets.size());
            for (InternalBucket bucket : buckets) {
                bucketMap.put(bucket.getKey(), bucket);
            }
        }
        return bucketMap.get(key);
    }

    @Override
    protected float doGetMaxScore() {
        // 懒加载并缓存
        float maxScoreLocal = Float.NaN;
        if (CommonUtil.isNotEmpty(buckets)) {
            for (InternalBucket bucket : buckets) {
                float bucketMaxScore = bucket.getMaxScore();
                if (maxScoreLocal < bucketMaxScore) {
                    maxScoreLocal = bucketMaxScore;
                }
            }
        }
        return maxScoreLocal;
    }

    public static class InternalBucket implements Filters.Bucket {

        private final String key;
        private long docCount;
        List<InternalAggregation> aggregations;
        private Float maxScore;

        public InternalBucket(String key, long docCount, List<InternalAggregation> aggregations) {
            this.key = key;
            this.docCount = docCount;
            this.aggregations = aggregations;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getKeyAsString() {
            return key;
        }

        @Override
        public long getDocCount() {
            return docCount;
        }

        @Override
        public List<InternalAggregation> getAggregations() {
            return aggregations;
        }

        @Override
        public float getMaxScore() {
            if(this.maxScore == null) {
                float maxScoreLocal = Float.NaN;
                if (CommonUtil.isNotEmpty(aggregations)) {
                    for (InternalAggregation aggregation : aggregations) {
                        float subMaxScore = aggregation.getMaxScore();
                        if (maxScoreLocal < subMaxScore) {
                            maxScoreLocal = subMaxScore;
                        }
                    }
                }
                this.maxScore = maxScoreLocal;
            }

            return this.maxScore;
        }
    }
}
