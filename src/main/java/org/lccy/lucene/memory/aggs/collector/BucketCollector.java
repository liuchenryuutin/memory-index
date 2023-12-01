package org.lccy.lucene.memory.aggs.collector;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ScoreMode;
import org.lccy.lucene.memory.aggs.leaf.LeafBucketCollector;
import org.lccy.lucene.memory.search.SearchContext;

import java.io.IOException;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/12/11 15:08 <br>
 * @author: liuchen11
 */
public abstract class BucketCollector implements Collector {

    public static final BucketCollector NO_OP_COLLECTOR = new BucketCollector() {

        @Override
        public LeafBucketCollector getLeafCollector(LeafReaderContext reader) {
            return LeafBucketCollector.NO_OP_COLLECTOR;
        }
        @Override
        public void preCollection(SearchContext searchContext) throws IOException {
            // no-op
        }
        @Override
        public void postCollection(SearchContext searchContext) throws IOException {
            // no-op
        }
        @Override
        public ScoreMode scoreMode() {
            return ScoreMode.COMPLETE_NO_SCORES;
        }
    };

    @Override
    public abstract LeafBucketCollector getLeafCollector(LeafReaderContext ctx) throws IOException;

    /**
     * 分组收集器prepare处理
     */
    public abstract void preCollection(SearchContext searchContext) throws IOException;

    /**
     * 分组收集器post处理
     */
    public abstract void postCollection(SearchContext searchContext) throws IOException;
}
