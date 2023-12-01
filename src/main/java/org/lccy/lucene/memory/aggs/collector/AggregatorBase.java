package org.lccy.lucene.memory.aggs.collector;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.ScoreMode;
import org.lccy.lucene.memory.aggs.collector.aggregation.InternalAggregation;
import org.lccy.lucene.memory.aggs.leaf.LeafBucketCollector;
import org.lccy.lucene.memory.search.SearchContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分组查询
 *
 * @Date: 2023/12/11 10:06 <br>
 * @author: liuchen11
 */
public abstract class AggregatorBase extends Aggregator {

    protected final String name;
    protected final Aggregator parent;
    protected final List<Aggregator> subAggregators;
    private Map<String, Aggregator> subAggregatorbyName;
    protected BucketCollector multiSubCollector;

    public AggregatorBase(String name, Aggregator parent) {
        this.name = name;
        this.parent = parent;
        this.subAggregators = new ArrayList<>();
    }

    public void addSubAggregator(Aggregator subAggregator) {
        this.subAggregators.add(subAggregator);
    }

    protected abstract LeafBucketCollector getLeafCollector(LeafReaderContext ctx, LeafBucketCollector sub) throws IOException;

    @Override
    public final LeafBucketCollector getLeafCollector(LeafReaderContext ctx) throws IOException {
        preGetSubLeafCollectors();
        if(multiSubCollector == null) {
            multiSubCollector = MultiBucketCollector.wrap(subAggregators);
        }
        final LeafBucketCollector sub = multiSubCollector.getLeafCollector(ctx);
        return getLeafCollector(ctx, sub);
    }

    protected void preGetSubLeafCollectors() throws IOException {
    }

    @Override
    public ScoreMode scoreMode() {
        for (BucketCollector sub : subAggregators) {
            if (sub.scoreMode().needsScores()) {
                return ScoreMode.COMPLETE;
            }
        }
        return ScoreMode.COMPLETE_NO_SCORES;
    }


    @Override
    public final void preCollection(SearchContext searchContext) throws IOException {
        doPreCollection(searchContext);
        if(multiSubCollector == null) {
            multiSubCollector = MultiBucketCollector.wrap(subAggregators);
        }
        multiSubCollector.preCollection(searchContext);
    }

    protected void doPreCollection(SearchContext searchContext) throws IOException {
    }

    @Override
    public final void postCollection(SearchContext searchContext) throws IOException {
        // post-collect this agg before subs to make it possible to buffer and then replay in postCollection()
        doPostCollection();
        if(multiSubCollector == null) {
            multiSubCollector = MultiBucketCollector.wrap(subAggregators);
        }
        multiSubCollector.postCollection(searchContext);
    }

    protected void doPostCollection() throws IOException {
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Aggregator parent() {
        return parent;
    }

    @Override
    public Aggregator subAggregator(String aggName) {
        if (subAggregatorbyName == null) {
            subAggregatorbyName = new HashMap<>(subAggregators.size());
            for (int i = 0; i < subAggregators.size(); i++) {
                subAggregatorbyName.put(subAggregators.get(i).name(), subAggregators.get(i));
            }
        }
        return subAggregatorbyName.get(aggName);
    }

    @Override
    public List<Aggregator> subAggregator() {
        return subAggregators;
    }

    protected final List<InternalAggregation> buildEmptySubAggregations() {
        List<InternalAggregation> aggs = new ArrayList<>();
        for (Aggregator aggregator : subAggregators) {
            aggs.add(aggregator.buildEmptyAggregation());
        }
        return aggs;
    }
}
