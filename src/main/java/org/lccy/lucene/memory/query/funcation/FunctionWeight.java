package org.lccy.lucene.memory.query.funcation;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.lccy.lucene.memory.exception.LuceneException;

import java.io.IOException;
import java.util.Set;

/**
 * FunctionScoreQuery的Weight类，调用ScoreFunction进行评分和执行计划
 *
 * @Date: 2023/12/02 20:30 <br>
 * @author: liuchen11
 */
public class FunctionWeight extends Weight {

    private final Weight subQueryWeight;
    private final ScoreFunction scoreFunction;
    private final CombineFunction combineFunction;
    private final float maxBoost;

    public FunctionWeight(Query parent, Weight subQueryWeight, ScoreFunction scoreFunction, CombineFunction combineFunction, float maxBoost) throws IOException {
        super(parent);
        this.subQueryWeight = subQueryWeight;
        this.scoreFunction = scoreFunction;
        this.combineFunction = combineFunction;
        this.maxBoost = maxBoost;
    }

    @Override
    public void extractTerms(Set<Term> terms) {
        subQueryWeight.extractTerms(terms);
    }

    @Override
    public Scorer scorer(LeafReaderContext context) throws IOException {
        Scorer subQueryScorer = subQueryWeight.scorer(context);
        if (subQueryScorer == null) {
            return null;
        }
        return new FunctionFactorScorer(context, this, subQueryScorer, scoreFunction, combineFunction, maxBoost);
    }

    @Override
    public Explanation explain(LeafReaderContext context, int doc) throws IOException {
        Explanation subExpl = subQueryWeight.explain(context, doc);
        if (!subExpl.isMatch()) {
            return subExpl;
        }
        return combineFunction.explain(subExpl, scoreFunction.explain(context, doc, subExpl), maxBoost);
    }

    @Override
    public boolean isCacheable(LeafReaderContext ctx) {
        return true;
    }

    public static class FunctionFactorScorer extends FilterScorer {

        private final ScoreFunction socreFunction;
        private final LeafReaderContext context;
        private final CombineFunction combineFunction;
        private final float maxBoost;

        private FunctionFactorScorer(LeafReaderContext context, FunctionWeight w, Scorer scorer, ScoreFunction scoreFunction
                , CombineFunction combineFunction, float maxBoost) throws IOException {
            super(scorer, w);
            this.context = context;
            this.socreFunction = scoreFunction;
            this.combineFunction = combineFunction;
            this.maxBoost = maxBoost;
        }

        @Override
        public float score() throws IOException {
            int docId = docID();
            float subQueryScore = super.score();

            double functionScore = socreFunction.score(context, docId, subQueryScore);
            float finalScore = combineFunction.combine(subQueryScore, functionScore, maxBoost);
            if (finalScore < 0f || Float.isNaN(finalScore)) {
                throw new LuceneException("function score query returned an invalid score: " + finalScore + " for doc: " + docId);
            }
            return finalScore;
        }

        @Override
        public float getMaxScore(int upTo) throws IOException {
            return maxBoost;
        }
    }

}

