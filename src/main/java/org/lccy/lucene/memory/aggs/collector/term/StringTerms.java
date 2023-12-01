package org.lccy.lucene.memory.aggs.collector.term;

import org.lccy.lucene.memory.aggs.collector.aggregation.Aggregation;
import org.lccy.lucene.memory.aggs.collector.aggregation.InternalAggregation;
import org.lccy.lucene.memory.util.CommonUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字符串term aggs分组结果实现类
 *
 * @Date: 2023/12/11 20:03 <br>
 * @author: liuchen11
 */
public class StringTerms extends InternalAggregation implements Terms {

    private final List<InternalBucket> buckets;
    private final long otherDocCount;
    private final long bucketCounts;
    private transient Map<String, InternalBucket> bucketMap;

    public StringTerms(String name, List<InternalBucket> buckets, long otherDocCount, long bucketCounts) {
        super(name);
        this.buckets = buckets;
        this.otherDocCount = otherDocCount;
        this.bucketCounts = bucketCounts;
    }

    @Override
    public List<? extends Bucket> getBuckets() {
        return buckets;
    }

    @Override
    public Bucket getBucketByKey(String term) {
        if (bucketMap == null) {
            bucketMap = new HashMap<>(buckets.size());
            for (InternalBucket bucket : buckets) {
                bucketMap.put(bucket.getKeyAsString(), bucket);
            }
        }
        return bucketMap.get(term);
    }

    @Override
    public long getOtherDocCounts() {
        return otherDocCount;
    }

    @Override
    public long getBucketCounts() {
        return bucketCounts;
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

    public static class InternalBucket implements Terms.Bucket {

        private String term;
        private long docCount;
        private List<InternalAggregation> aggregations;
        private Float maxScore;

        public InternalBucket(String term, long docCount, List<InternalAggregation> aggregations) {
            this.term = term;
            this.docCount = docCount;
            this.aggregations = aggregations;
        }

        @Override
        public Object getKey() {
            return term;
        }

        @Override
        public String getKeyAsString() {
            return term;
        }

        @Override
        public long getDocCount() {
            return docCount;
        }

        @Override
        public List<? extends Aggregation> getAggregations() {
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
