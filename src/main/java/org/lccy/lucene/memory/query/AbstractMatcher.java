package org.lccy.lucene.memory.query;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Explanation;

/**
 * CustomQuery的匹配项
 *
 * @Date: 2023/11/25 21:16 <br>
 * @author: liuchen11
 */
public abstract class AbstractMatcher<T> {

    public float score(LeafReaderContext context, T term, float boost) {
        return doScore(context, term, boost);
    }

    public Explanation explain(LeafReaderContext context, int doc) {
        return doExplain(context, doc);
    }

    public abstract boolean match(String json, T term);

    public abstract float doScore(LeafReaderContext context, T term, float boost);

    public abstract Explanation doExplain(LeafReaderContext context, int doc);
}
