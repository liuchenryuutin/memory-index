package org.lccy.lucene.memory.aggs.collector.filter;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;
import org.lccy.lucene.memory.aggs.collector.Aggregator;
import org.lccy.lucene.memory.aggs.collector.BucketsAggregator;
import org.lccy.lucene.memory.aggs.collector.aggregation.InternalAggregation;
import org.lccy.lucene.memory.aggs.leaf.LeafBucketCollector;
import org.lccy.lucene.memory.aggs.leaf.LeafBucketCollectorBase;
import org.lccy.lucene.memory.aggs.sort.BucketsSort;
import org.lccy.lucene.memory.exception.QueryException;
import org.lccy.lucene.memory.search.SearchContext;
import org.lccy.lucene.memory.util.Lucene;
import org.lccy.lucene.memory.util.OrderedQueue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Filters分组收集器，参照Elasticsearch
 *
 * @Date: 2023/12/11 16:19 <br>
 * @author: liuchen11
 */
public class FiltersAggregator extends BucketsAggregator {

    private final String[] keys;
    private final Query[] querys;
    private final boolean showOtherBucket;
    private final String otherBucketKey;
    private final int totalNumKeys;
    private Weight[] weights;

    public FiltersAggregator(String name, Aggregator parent, List<KeyedFilter> filters, String otherBucketKey, BucketsSort bucketsSort) {
        super(name, parent, bucketsSort);
        String[] keyArr = new String[filters.size()];
        Query[] queryArr = new Query[filters.size()];
        for(int i = 0; i < filters.size(); i++) {
            KeyedFilter filter = filters.get(i);
            keyArr[i] = filter.getKey();
            queryArr[i] = filter.getQuery();
        }
        this.keys = keyArr;
        this.querys = queryArr;
        this.showOtherBucket = otherBucketKey != null;
        this.otherBucketKey = otherBucketKey;
        if (showOtherBucket) {
            totalNumKeys = keys.length + 1;
        } else {
            totalNumKeys = keys.length;
        }
    }

    @Override
    protected void doPreCollection(SearchContext searchContext) throws IOException {
        super.doPreCollection(searchContext);
        this.weights = getWeights(name, querys, searchContext.getSearcher());
    }

    protected Weight[] getWeights(String aggsName, Query[] filters, IndexSearcher searcher) {
        Weight[] weights = null;
        try {
            weights = new Weight[filters.length];
            for (int i = 0; i < filters.length; ++i) {
                weights[i] = searcher.createWeight(searcher.rewrite(filters[i]), ScoreMode.COMPLETE_NO_SCORES, 1);
            }
        } catch (IOException e) {
            throw new QueryException("Failed to initialse filters for aggregation [" + aggsName + "]", e);
        }
        return weights;
    }

    @Override
    protected LeafBucketCollector getLeafCollector(LeafReaderContext ctx, LeafBucketCollector sub) throws IOException {
        // no need to provide deleted docs to the filter
        final Bits[] bits = new Bits[weights.length];
        for (int i = 0; i < weights.length; ++i) {
            bits[i] = Lucene.asSequentialAccessBits(ctx.reader().maxDoc(), weights[i].scorerSupplier(ctx));
        }
        return new LeafBucketCollectorBase(sub) {
            @Override
            public void collect(int doc, long bucket) throws IOException {
                boolean matched = false;
                for (int i = 0; i < bits.length; i++) {
                    if (bits[i].get(doc)) {
                        collectBucket(sub, doc, bucketOrd(bucket, i));
                        matched = true;
                    }
                }
                if (showOtherBucket && !matched) {
                    collectBucket(sub, doc, bucketOrd(bucket, bits.length));
                }
            }
        };
    }

    final long bucketOrd(long owningBucketOrdinal, int filterOrd) {
        return owningBucketOrdinal * totalNumKeys + filterOrd;
    }

    @Override
    public InternalAggregation doBuildAggregation(long owningBucketOrdinal) throws IOException {
        final List<InternalFilters.InternalBucket> buckets = new ArrayList<>();
        if (bucketsSort != null) {
            int bucketSize = showOtherBucket ? keys.length + 1 : keys.length;
            OrderedQueue<InternalFilters.InternalBucket> orderedBuckets = new OrderedQueue<InternalFilters.InternalBucket>(bucketSize, bucketsSort.getComparator());
            for (int i = 0; i < keys.length; i++) {
                long bucketOrd = bucketOrd(owningBucketOrdinal, i);
                InternalFilters.InternalBucket bucket = new InternalFilters.InternalBucket(keys[i], bucketDocCount(bucketOrd),
                        bucketAggregations(bucketOrd));
                orderedBuckets.insertWithOverflow(bucket);
            }
            // other bucket
            if (showOtherBucket) {
                long bucketOrd = bucketOrd(owningBucketOrdinal, keys.length);
                InternalFilters.InternalBucket bucket = new InternalFilters.InternalBucket(otherBucketKey, bucketDocCount(bucketOrd),
                        bucketAggregations(bucketOrd));
                orderedBuckets.add(bucket);
            }

            for (int i = orderedBuckets.size() - 1; i >= 0; --i) {
                final InternalFilters.InternalBucket bucket = orderedBuckets.pop();
                buckets.add(bucket);
            }
        } else {
            for (int i = 0; i < keys.length; i++) {
                long bucketOrd = bucketOrd(owningBucketOrdinal, i);
                InternalFilters.InternalBucket bucket = new InternalFilters.InternalBucket(keys[i], bucketDocCount(bucketOrd),
                        bucketAggregations(bucketOrd));
                buckets.add(bucket);
            }
        }

        return new InternalFilters(name, buckets);
    }

    public InternalAggregation doBuildEmptyAggregation() {
        List<InternalAggregation> subAggs = buildEmptySubAggregations();
        List<InternalFilters.InternalBucket> buckets = new ArrayList<>(keys.length);
        for (int i = 0; i < keys.length; i++) {
            InternalFilters.InternalBucket bucket = new InternalFilters.InternalBucket(keys[i], 0, subAggs);
            buckets.add(bucket);
        }

        if (showOtherBucket) {
            InternalFilters.InternalBucket bucket = new InternalFilters.InternalBucket(otherBucketKey, 0, subAggs);
            buckets.add(bucket);
        }

        return new InternalFilters(name, buckets);
    }
}
