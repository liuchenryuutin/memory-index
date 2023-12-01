package org.lccy.lucene.memory.aggs.collector;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.*;
import org.lccy.lucene.memory.aggs.leaf.LeafBucketCollector;
import org.lccy.lucene.memory.search.SearchContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 组合多个子分组的复合Collector
 *
 * @Date: 2023/12/11 15:27 <br>
 * @author: liuchen11
 */
public class MultiBucketCollector extends BucketCollector {

    public static BucketCollector wrap(Iterable<? extends BucketCollector> collectors) {
        // For the user's convenience, we allow NO_OP collectors to be passed.
        // However, to improve performance, these null collectors are found
        // and dropped from the array we save for actual collection time.
        int n = 0;
        for (BucketCollector c : collectors) {
            if (c != NO_OP_COLLECTOR) {
                n++;
            }
        }

        if (n == 0) {
            return NO_OP_COLLECTOR;
        } else if (n == 1) {
            // only 1 Collector - return it.
            BucketCollector col = null;
            for (BucketCollector c : collectors) {
                if (c != null) {
                    col = c;
                    break;
                }
            }
            return col;
        } else {
            BucketCollector[] colls = new BucketCollector[n];
            n = 0;
            for (BucketCollector c : collectors) {
                if (c != null) {
                    colls[n++] = c;
                }
            }
            return new MultiBucketCollector(colls);
        }
    }

    private final boolean cacheScores;
    private final BucketCollector[] collectors;


    private MultiBucketCollector(BucketCollector... collectors) {
        this.collectors = collectors;
        int numNeedsScores = 0;
        for (Collector collector : collectors) {
            if (collector.scoreMode().needsScores()) {
                numNeedsScores += 1;
            }
        }
        this.cacheScores = numNeedsScores >= 2;
    }

    @Override
    public ScoreMode scoreMode() {
        ScoreMode scoreMode = null;
        for (Collector collector : collectors) {
            if (scoreMode == null) {
                scoreMode = collector.scoreMode();
            } else if (scoreMode != collector.scoreMode()) {
                return ScoreMode.COMPLETE;
            }
        }
        return scoreMode;
    }

    @Override
    public void preCollection(SearchContext searchContext) throws IOException {
        for (BucketCollector collector : collectors) {
            collector.preCollection(searchContext);
        }
    }

    @Override
    public void postCollection(SearchContext searchContext) throws IOException {
        for (BucketCollector collector : collectors) {
            collector.postCollection(searchContext);
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(collectors);
    }

    @Override
    public LeafBucketCollector getLeafCollector(LeafReaderContext context) throws IOException {
        final List<LeafBucketCollector> leafCollectors = new ArrayList<>();
        for (BucketCollector collector : collectors) {
            final LeafBucketCollector leafCollector;
            try {
                leafCollector = collector.getLeafCollector(context);
            } catch (CollectionTerminatedException e) {
                // this leaf collector does not need this segment
                continue;
            }
            leafCollectors.add(leafCollector);
        }
        switch (leafCollectors.size()) {
            case 0:
                throw new CollectionTerminatedException();
            case 1:
                return leafCollectors.get(0);
            default:
                return new MultiLeafBucketCollector(leafCollectors, cacheScores);
        }
    }

    private static class MultiLeafBucketCollector extends LeafBucketCollector {

        private final boolean cacheScores;
        private final LeafBucketCollector[] collectors;
        private int numCollectors;

        private MultiLeafBucketCollector(List<LeafBucketCollector> collectors, boolean cacheScores) {
            this.collectors = collectors.toArray(new LeafBucketCollector[collectors.size()]);
            this.cacheScores = cacheScores;
            this.numCollectors = this.collectors.length;
        }

        @Override
        public void setScorer(Scorable scorer) throws IOException {
            if (cacheScores) {
                scorer = new ScoreCachingWrappingScorer(scorer);
            }
            for (int i = 0; i < numCollectors; ++i) {
                final LeafCollector c = collectors[i];
                c.setScorer(scorer);
            }
        }

        private void removeCollector(int i) {
            System.arraycopy(collectors, i + 1, collectors, i, numCollectors - i - 1);
            --numCollectors;
            collectors[numCollectors] = null;
        }

        @Override
        public void collect(int doc, long bucket) throws IOException {
            final LeafBucketCollector[] collectors = this.collectors;
            int numCollectors = this.numCollectors;
            for (int i = 0; i < numCollectors; ) {
                final LeafBucketCollector collector = collectors[i];
                try {
                    collector.collect(doc, bucket);
                    ++i;
                } catch (CollectionTerminatedException e) {
                    removeCollector(i);
                    numCollectors = this.numCollectors;
                    if (numCollectors == 0) {
                        throw new CollectionTerminatedException();
                    }
                }
            }
        }
    }
}
