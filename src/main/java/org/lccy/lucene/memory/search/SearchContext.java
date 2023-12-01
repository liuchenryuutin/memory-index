package org.lccy.lucene.memory.search;

import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocsCollector;
import org.lccy.lucene.memory.aggs.collector.Aggregator;
import org.lccy.lucene.memory.index.config.IndexConfig;

import java.util.List;

/**
 * 查询上下文
 *
 * @Date: 2023/12/15 18:17 <br>
 * @author: liuchen11
 */
@Getter
@Setter
public class SearchContext {

    private SearchRequest request;

    private IndexSearcher searcher;

    private Query query;

    private Sort sort;

    private IndexConfig indexConfig;

    private TopDocsCollector topCollector;

    private List<Aggregator> aggregators;

}
