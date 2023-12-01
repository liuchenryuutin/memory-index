package org.lccy.lucene.memory.index.config;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.similarities.BM25Similarity;

import java.io.IOException;

/**
 * SearcherManager调用，用于生成IndexSearcher
 *
 * @Date: 2023/11/23 10:19 <br>
 * @author: liuchen11
 */
public class CustomSearcherFactory extends SearcherFactory {

    @Override
    public IndexSearcher newSearcher(IndexReader reader, IndexReader previousReader) throws IOException {
        IndexSearcher searcher = super.newSearcher(reader, previousReader);
        searcher.setSimilarity(new BM25Similarity());
        return searcher;
    }
}
