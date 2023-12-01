package org.lccy.lucene.memory.aggs.collector;

import org.apache.lucene.search.Scorable;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;

import java.io.IOException;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/12/13 10:00 <br>
 * @author: liuchen11
 */
public class MaxScoreCollector extends SimpleCollector {
    private Scorable scorer;
    private float maxScore = Float.NEGATIVE_INFINITY;
    private boolean hasHits = false;

    @Override
    public void setScorer(Scorable scorer) {
        this.scorer = scorer;
    }

    @Override
    public ScoreMode scoreMode() {
        // Could be TOP_SCORES but it is always used in a MultiCollector anyway, so this saves some wrapping.
        return ScoreMode.COMPLETE;
    }

    @Override
    public void collect(int doc) throws IOException {
        hasHits = true;
        maxScore = Math.max(maxScore, scorer.score());
    }

    /**
     * Get the maximum score. This returns {@link Float#NaN} if no hits were
     * collected.
     */
    public float getMaxScore() {
        return hasHits ? maxScore : Float.NaN;
    }

}
