package org.lccy.lucene.memory.query.funcation;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.util.Objects;

/**
 * 自定义评分查询，内部调用ScoreFunction完成文档评分的计算
 *
 * @Date: 2023/12/02 19:21 <br>
 * @author: liuchen11
 */
public class FunctionScoreQuery extends Query {

    private final Query subQuery;
    private final ScoreFunction scoreFunction;
    private final CombineFunction combineFunction;
    private final float maxBoost;

    public FunctionScoreQuery(Query subQuery, ScoreFunction scoreFunction, CombineFunction combineFunction, float maxBoost) {
        this.subQuery = subQuery;
        this.scoreFunction = scoreFunction;
        this.combineFunction = combineFunction;
        this.maxBoost = maxBoost;
    }

    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        Query rewritten = super.rewrite(reader);
        if (rewritten != this) {
            return rewritten;
        }
        Query newQ = subQuery.rewrite(reader);
        if (newQ != subQuery) {
            return new FunctionScoreQuery(newQ, scoreFunction, combineFunction, maxBoost);
        }
        return this;
    }

    @Override
    public void visit(QueryVisitor visitor) {
        subQuery.visit(visitor.getSubVisitor(BooleanClause.Occur.MUST, this));
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
        if (scoreMode == ScoreMode.COMPLETE_NO_SCORES) {
            return subQuery.createWeight(searcher, scoreMode, boost);
        }
        Weight subQueryWeight = subQuery.createWeight(searcher, scoreMode, boost);
        return new FunctionWeight(this, subQueryWeight, scoreFunction, combineFunction, maxBoost);
    }

    @Override
    public String toString(String field) {
        StringBuilder sb = new StringBuilder();
        sb.append("function score (").append(subQuery.toString(field)).append(", functions: ").append(scoreFunction.toString()).append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (sameClassAs(obj) == false) {
            return false;
        }
        FunctionScoreQuery other = (FunctionScoreQuery) obj;
        return Objects.equals(this.subQuery, other.subQuery) && Objects.equals(this.scoreFunction, other.scoreFunction)
                && Objects.equals(this.combineFunction, other.combineFunction) && Objects.equals(this.maxBoost, other.maxBoost);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classHash(), subQuery, scoreFunction, combineFunction, maxBoost);
    }

}