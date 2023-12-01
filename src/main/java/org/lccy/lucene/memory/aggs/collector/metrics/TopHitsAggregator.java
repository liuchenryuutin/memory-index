package org.lccy.lucene.memory.aggs.collector.metrics;

import com.carrotsearch.hppc.LongObjectHashMap;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.*;
import org.lccy.lucene.memory.aggs.collector.Aggregator;
import org.lccy.lucene.memory.aggs.collector.MaxScoreCollector;
import org.lccy.lucene.memory.aggs.collector.aggregation.InternalAggregation;
import org.lccy.lucene.memory.aggs.leaf.LeafBucketCollector;
import org.lccy.lucene.memory.aggs.leaf.LeafBucketCollectorBase;
import org.lccy.lucene.memory.builder.SearchHitsBuilder;
import org.lccy.lucene.memory.search.SearchContext;
import org.lccy.lucene.memory.search.SearchHits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/12/13 09:19 <br>
 * @author: liuchen11
 */
public class TopHitsAggregator extends MetricsAggregator {

    private final TopHitsAggsConfig topHitsAggs;
    private final LongObjectHashMap<Collectors> topDocsCollectors;
    private SearchContext searchContext;

    public TopHitsAggregator(String name, Aggregator parent, TopHitsAggsConfig context) {
        super(name, parent);
        topHitsAggs = context;
        topDocsCollectors = new LongObjectHashMap<>();
    }

    @Override
    protected void doPreCollection(SearchContext searchContext) throws IOException {
        super.doPreCollection(searchContext);
        this.searchContext = searchContext;
    }

    @Override
    public ScoreMode scoreMode() {
        Sort sort = topHitsAggs.getSort(searchContext.getIndexConfig());
        if (sort != null) {
            return sort.needsScores() || topHitsAggs.isTrackScores() ? ScoreMode.COMPLETE : ScoreMode.COMPLETE_NO_SCORES;
        } else {
            // sort by score
            return ScoreMode.COMPLETE;
        }
    }

    @Override
    protected LeafBucketCollector getLeafCollector(LeafReaderContext ctx, LeafBucketCollector sub) throws IOException {
        // 延迟加载，收集时，判断LeafCollector不存在，则创建并缓存到map中
        final Map<Long, LeafCollector> leafCollectors = new HashMap<>(4);
        return new LeafBucketCollectorBase(sub) {
            Scorable scorer;

            @Override
            public void setScorer(Scorable scorer) throws IOException {
                super.setScorer(scorer);
                this.scorer = scorer;
            }

            @Override
            public void collect(int docId, long bucket) throws IOException {
                TopHitsAggregator.Collectors collectors = topDocsCollectors.get(bucket);
                if (collectors == null) {
                    Sort sort = topHitsAggs.getSort(searchContext.getIndexConfig());
                    int numHits = topHitsAggs.getFrom() + topHitsAggs.getSize();
                    numHits = Math.min(numHits, searchContext.getSearcher().getIndexReader().maxDoc());
                    if (sort == null) {
                        collectors = new TopHitsAggregator.Collectors(TopScoreDocCollector.create(numHits, Integer.MAX_VALUE), new MaxScoreCollector());
                    } else {
                        collectors = new TopHitsAggregator.Collectors(
                                TopFieldCollector.create(sort, numHits, Integer.MAX_VALUE),
                                topHitsAggs.isTrackScores() ? new MaxScoreCollector() : null);
                    }
                    topDocsCollectors.put(bucket, collectors);
                }

                LeafCollector leafCollector = leafCollectors.get(bucket);
                if (leafCollector == null) {
                    leafCollector = collectors.collector.getLeafCollector(ctx);
                    if (scorer != null) {
                        leafCollector.setScorer(scorer);
                    }
                    leafCollectors.put(bucket, leafCollector);
                }
                leafCollector.collect(docId);
            }
        };
    }

    @Override
    public InternalAggregation doBuildAggregation(long bucket) throws IOException {
        Collectors collectors = topDocsCollectors.get(bucket);
        if (collectors == null) {
            return buildEmptyAggregation();
        }
        TopDocsCollector<?> topDocsCollector = collectors.topDocsCollector;
        TopDocs topDocs = topDocsCollector.topDocs();
        float maxScore = Float.NaN;
        if (topHitsAggs.getSort(searchContext.getIndexConfig()) == null) {
            // 无排序的情况下，默认按照最大评分排序，所以取第一个就OK
            if (topDocs.scoreDocs.length > 0) {
                maxScore = topDocs.scoreDocs[0].score;
            }
        } else if (topHitsAggs.isTrackScores()) {
            // 有排序的情况下，取最大评分收集器的评分值
//            TopFieldCollector.populateScores(topDocs.scoreDocs, subSearchContext.searcher(), subSearchContext.query());
            maxScore = collectors.maxScoreCollector.getMaxScore();
        }

        SearchHits hits = SearchHitsBuilder.buildHits(topDocs, searchContext, topHitsAggs);

        return new InternalTopHits(name, hits, maxScore);
    }

    @Override
    public InternalAggregation doBuildEmptyAggregation() {
        SearchHits hits = new SearchHits();
        Sort sort = topHitsAggs.getSort(searchContext.getIndexConfig());
        if (sort != null) {
            hits.setSortFields(sort.getSort());
        }
        hits.setDocuments(new ArrayList<>(0));
        return new InternalTopHits(name, hits, Float.NaN);
    }


    private static class Collectors {
        // topN收集器
        public final TopDocsCollector<?> topDocsCollector;
        // 最大评分收集器
        public final MaxScoreCollector maxScoreCollector;
        // 组合上面两种
        public final Collector collector;

        Collectors(TopDocsCollector<?> topDocsCollector, MaxScoreCollector maxScoreCollector) {
            this.topDocsCollector = topDocsCollector;
            this.maxScoreCollector = maxScoreCollector;
            this.collector = MultiCollector.wrap(topDocsCollector, maxScoreCollector);
        }
    }

}
