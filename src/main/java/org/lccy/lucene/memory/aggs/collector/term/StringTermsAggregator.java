package org.lccy.lucene.memory.aggs.collector.term;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.lccy.lucene.memory.aggs.collector.Aggregator;
import org.lccy.lucene.memory.aggs.collector.aggregation.InternalAggregation;
import org.lccy.lucene.memory.aggs.leaf.LeafBucketCollector;
import org.lccy.lucene.memory.aggs.leaf.LeafBucketCollectorBase;
import org.lccy.lucene.memory.aggs.sort.BucketsSort;
import org.lccy.lucene.memory.util.OrderedQueue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字符串terms分组查询器
 *
 * @Date: 2023/12/11 18:30 <br>
 * @author: liuchen11
 */
public class StringTermsAggregator extends TermsAggregator {

    private Map<String, Integer> bucketOrds = new HashMap<>();
    private long otherDocCount = 0;

    public StringTermsAggregator(String name, String field, Aggregator parent, BucketsSort bucketsSort) {
        super(name, field, parent, bucketsSort);
    }

    @Override
    protected LeafBucketCollector getLeafCollector(LeafReaderContext ctx, LeafBucketCollector sub) throws IOException {
        SortedSetDocValues values = ctx.reader().getSortedSetDocValues(field);
        return new LeafBucketCollectorBase(sub) {
            final BytesRefBuilder previous = new BytesRefBuilder();

            @Override
            public void collect(int doc, long bucket) throws IOException {
                assert bucket == 0;
                if (values.advanceExact(doc)) {

                    // 进行去重
                    previous.clear();
                    long ord;
                    boolean first = true;
                    while ((ord = values.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {
                        final BytesRef bytes = values.lookupOrd(ord);
                        if (first) {
                            first = false;
                        } else if (previous.get().equals(bytes)) {
                            continue;
                        }

                        String value = bytes.utf8ToString();
                        int bucketOrdinal;
                        if (bucketOrds.containsKey(value)) {
                            bucketOrdinal = bucketOrds.get(value);
                            collectExistingBucket(sub, doc, bucketOrdinal);
                        } else {
                            bucketOrdinal = bucketOrds.size();
                            bucketOrds.put(value, bucketOrdinal);
                            collectBucket(sub, doc, bucketOrdinal);
                        }
                        previous.copyBytes(bytes);
                    }
                } else {
                    otherDocCount++;
                }
            }
        };
    }

    @Override
    public InternalAggregation doBuildAggregation(long owningBucketOrdinal) throws IOException {
        assert owningBucketOrdinal == 0;

        BucketsSort bucketsSorter = bucketsSort;
        if(bucketsSorter == null) {
            bucketsSorter = BucketsSort.COUNT_DESC;
        }
        int bucketSize = bucketOrds.size();
        OrderedQueue<StringTerms.InternalBucket> orderedBuckets = new OrderedQueue<StringTerms.InternalBucket>(bucketSize, bucketsSorter.getComparator());
        for (Map.Entry<String, Integer> bucketOrd : bucketOrds.entrySet()) {
            String term = bucketOrd.getKey();
            Integer bucketOrdinal = bucketOrd.getValue();
            StringTerms.InternalBucket bucket = new StringTerms.InternalBucket(term, bucketDocCount(bucketOrdinal), bucketAggregations(bucketOrdinal));

            orderedBuckets.insertWithOverflow(bucket);
        }

        // Get the top size buckets
        final List<StringTerms.InternalBucket> list = new ArrayList<>(bucketSize);
        int gotSize = size == -1 ? bucketSize : size;
        for (int i = bucketSize - 1; i >= bucketSize - gotSize; --i) {
            final StringTerms.InternalBucket bucket = orderedBuckets.pop();
            list.add(bucket);
        }

        return new StringTerms(name, list, otherDocCount, bucketSize);
    }

    @Override
    public InternalAggregation doBuildEmptyAggregation() {
        return new StringTerms(name, new ArrayList<>(0), 0, 0);
    }
}
