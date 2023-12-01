package org.lccy.lucene.memory.aggs.leaf;

import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorable;

import java.io.IOException;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/12/11 15:10 <br>
 * @author: liuchen11
 */
public abstract class LeafBucketCollector implements LeafCollector {

    public static final LeafBucketCollector NO_OP_COLLECTOR = new LeafBucketCollector() {
        @Override
        public void setScorer(Scorable arg0) throws IOException {
            // no-op
        }
        @Override
        public void collect(int doc, long bucket) {
            // no-op
        }
    };

    public abstract void collect(int doc, long bucket) throws IOException;

    @Override
    public final void collect(int doc) throws IOException {
        collect(doc, 0);
    }

    @Override
    public void setScorer(Scorable scorer) throws IOException {
        // no-op by default
    }

}
