package org.lccy.lucene.memory.query;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.Set;

/**
 * CustomQuery的Weight类，对索引内容调用AbstractMatcher进行匹配、评分
 *
 * @Date: 2023/11/25 20:28 <br>
 * @author: liuchen11
 */
public class CustomWeight<T> extends Weight {

    protected final CustomQuery query;
    protected final Similarity similarity;
    private final float boost;

    public CustomWeight(CustomQuery<T> query, IndexSearcher searcher, float boost) {
        super(query);
        this.similarity = searcher.getSimilarity();
        this.query = query;
        this.boost = boost;
    }

    @Override
    public void extractTerms(Set<Term> terms) {

    }

    @Override
    public Explanation explain(LeafReaderContext context, int doc) throws IOException {
        // 查询的评分
        return query.getMatcher().explain(context, doc);
    }

    @Override
    public Scorer scorer(LeafReaderContext context) throws IOException {
        // 创建自定义的 Scorer 对象来执行实际的匹配逻辑
        LeafReader reader = context.reader();
        Terms terms = reader.terms(query.getField());
        IntArrayList docIds = new IntArrayList();
        IntIntMap docIdIdxMap = new IntIntHashMap();
        int idx = 0;
        if(terms != null) {
            TermsEnum termsEnum = terms.iterator();
            BytesRef term;
            while ((term = termsEnum.next()) != null) {
                String termText = term.utf8ToString();

                if(query.getMatcher().match(termText, query.getTerm())) {
                    PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.NONE);
                    int docID;
                    while ((docID = postingsEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                        if(!docIdIdxMap.containsKey(docID)) {
                            docIds.add(docID);
                            docIdIdxMap.put(docID, idx);
                            idx++;
                        }
                    }
                }
            }
        }
        CustomDocIdSetIterator docIdSetIterator = new CustomDocIdSetIterator(docIds, docIdIdxMap);
        Scorer result = new CustomScorer(this, context, docIdSetIterator, query.getMatcher(), boost);
        return result;
    }

    @Override
    public boolean isCacheable(LeafReaderContext ctx) {
        return true;
    }

    public class CustomScorer extends Scorer {

        private LeafReaderContext context;
        private CustomWeight customWeight;
        private CustomDocIdSetIterator iterator;
        private AbstractMatcher matcher;
        private float boost;

        protected CustomScorer(CustomWeight weight, LeafReaderContext context, CustomDocIdSetIterator iterator, AbstractMatcher matcher, float boost) {
            super(weight);
            this.customWeight = weight;
            this.context = context;
            this.iterator = iterator;
            this.matcher = matcher;
            this.boost = boost;
        }

        @Override
        public DocIdSetIterator iterator() {
            return iterator;
        }

        @Override
        public float getMaxScore(int upTo) throws IOException {
            return Float.MAX_VALUE;
        }

        @Override
        public float score() throws IOException {
            return matcher.score(context, customWeight.query.getTerm(), this.boost);
        }

        @Override
        public int docID() {
            return iterator.docID();
        }
    }
}
