package org.lccy.lucene.memory.query;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;

import java.io.IOException;

/**
 * 自定义查询器
 *
 * @Date: 2023/11/25 08:41 <br>
 * @author: liuchen11
 */
public class CustomQuery<T> extends Query {

    private AbstractMatcher matcher;
    private String field;
    private T term;

    public CustomQuery(String field, AbstractMatcher<T> matcher, T term) {
        if(matcher == null || term == null) {
            throw new IllegalArgumentException("CustomQuery must has matcher and term");
        }
        this.field = field;
        this.matcher = matcher;
        this.term = term;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
        CustomWeight weight = new CustomWeight(this, searcher, boost);
        return weight;
    }

    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        return this;
    }

    @Override
    public void visit(QueryVisitor visitor) {
        if (visitor.acceptField(field)) {
            visitor.visitLeaf(this);
        }
    }

    @Override
    public String toString(String field) {
        StringBuilder buffer = new StringBuilder();
        if (!field.equals(field)) {
            buffer.append(field);
            buffer.append(":");
        }
        buffer.append(term);
        return buffer.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return sameClassAs(obj) && term.equals((T) obj);
    }

    @Override
    public int hashCode() {
        return classHash() ^ term.hashCode();
    }

    public String getField() {
        return field;
    }

    public AbstractMatcher getMatcher() {
        return matcher;
    }

    public T getTerm() {
        return term;
    }
}
