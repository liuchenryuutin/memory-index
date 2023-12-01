package org.lccy.lucene.memory.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.lccy.lucene.memory.aggs.collector.Aggregator;
import org.lccy.lucene.memory.aggs.collector.aggregation.InternalAggregation;
import org.lccy.lucene.memory.builder.DocumentBuilder;
import org.lccy.lucene.memory.builder.QueryBuilder;
import org.lccy.lucene.memory.builder.SearchHitsBuilder;
import org.lccy.lucene.memory.builder.SortBuilder;
import org.lccy.lucene.memory.constants.Constants;
import org.lccy.lucene.memory.exception.LuceneException;
import org.lccy.lucene.memory.exception.QueryException;
import org.lccy.lucene.memory.index.config.CustomSearcherFactory;
import org.lccy.lucene.memory.index.config.IndexConfig;
import org.lccy.lucene.memory.index.mapping.IndexFieldMapping;
import org.lccy.lucene.memory.index.mapping.IndexSettingMapping;
import org.lccy.lucene.memory.loader.IndexDataLoader;
import org.lccy.lucene.memory.search.*;
import org.lccy.lucene.memory.util.CollectionUtils;
import org.lccy.lucene.memory.util.CommonUtil;
import org.lccy.lucene.memory.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基于lucene的内存倒序索引，支持导入、重新导入、插入、更新、删除、查询操作<br/>
 * 查询支持分组查询、自定义查询、评分查询等方式<br/>
 * 注意：保存的数据量不宜过大，数据量过多，考虑使用elasticsearch、solr等可靠的基于文件系统的方案<br/>
 * 注意：插入/更新是整体覆盖更新，不是只更新某些字段!!!<br/>
 * 每次必须给出全量字段，只更新某些字段现在还没有做，等有空了做
 *
 * @Date: 2023/11/21 09:08 <br>
 * @author: liuchen11
 */
public class MemoryIndex {

    private static final Logger logger = LoggerFactory.getLogger(MemoryIndex.class);
    protected static final int TOTAL_HITS_THRESHOLD = 10000;

    protected Directory directory;
    protected IndexConfig indexConfig;
    protected IndexWriter indexWriter;
    protected SearcherManager searcherManager;
    protected IndexDataLoader indexDataLoader;
    // reload时禁止其他操作（插入、更新、删除、查询）
    protected final ReadWriteLock reloadLock = new ReentrantReadWriteLock();
    protected ScheduledExecutorService schedule = Executors.newScheduledThreadPool(1);

    public MemoryIndex(IndexConfig indexConfig, IndexDataLoader loader) throws LuceneException {
        if (indexConfig == null) {
            throw new IllegalArgumentException("MemoryIndex config must set.");
        }
        this.indexConfig = indexConfig;
        this.indexDataLoader = loader;
        createIndexAndLoad();
        addRefreshTask();
        destroy();
    }

    /**
     * 创建索引并导入初期数据
     *
     * @throws IOException
     */
    private void createIndexAndLoad() throws LuceneException {
        try {
            this.directory = new ByteBuffersDirectory();
            Analyzer defAnalyzer = new KeywordAnalyzer();
            Map<String, Analyzer> fieldAnalyzers = indexConfig.getFieldAnalyzers();
            PerFieldAnalyzerWrapper analyzerWrapper = new PerFieldAnalyzerWrapper(defAnalyzer, fieldAnalyzers);
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzerWrapper);
            indexWriterConfig.setSimilarity(new BM25Similarity());
            this.indexWriter = new IndexWriter(directory, indexWriterConfig);

            if (indexDataLoader != null) {
                List<Document> documents = indexDataLoader.load(indexConfig);
                for (Document document : documents) {
                    this.indexWriter.addDocument(document);
                }
                this.indexWriter.commit();
            }
            this.searcherManager = new SearcherManager(this.indexWriter, new CustomSearcherFactory());
        } catch (IOException ex) {
            throw new LuceneException("MemoryIndex create/load error", ex);
        }
    }

    /**
     * 定时任务，定期刷新
     */
    public void addRefreshTask() {
        IndexSettingMapping indexSetting = this.indexConfig.getIndexSetting();
        this.schedule.scheduleAtFixedRate(() -> {
            try {
                maybeRefresh();
            } catch (Exception ex) {
                logger.warn("MemoryIndex refresh error:{}", ex.getMessage(), ex);
            }
        }, indexSetting.getRefreshInterval(), indexSetting.getRefreshInterval(), TimeUnit.MILLISECONDS);
    }

    /**
     * 销毁前释放资源
     */
    private void destroy() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("MemoryIndex destroy.");
            try {
                if (this.indexWriter != null) {
                    this.indexWriter.close();
                }
            } catch (IOException e) {
                logger.error("MemoryIndex destroy indexWriter error", e);
            }
            try {
                if (this.searcherManager != null) {
                    this.searcherManager.close();
                }
            } catch (IOException e) {
                logger.error("MemoryIndex destroy searcherManager error", e);
            }
        }));
    }

    /**
     * 插入单个文档到内存索引，必须给出主键字段<br/>
     * 注意：是整体覆盖更新，不是只更新某些字段!!!<br/>
     * 每次必须给出全量字段，只更新某些字段现在还没有做，等有空了做
     *
     * @param document
     * @throws IOException
     */
    public int insertUpdate(Map<String, Object> document) throws LuceneException {

        if (document == null || document.isEmpty()) {
            return 0;
        }
        String primaryName = indexConfig.getPrimaryField().getName();
        if (!document.containsKey(primaryName)) {
            throw new IllegalArgumentException("MemoryIndex insert/update must has primary field:" + primaryName);
        }

        boolean lock = false;
        try {
            lock = reloadLock.readLock().tryLock(10, TimeUnit.SECONDS);
            if (!lock) {
                throw new LuceneException("MemoryIndex is reloading, cannot insert/update.");
            }

            Document insert = DocumentBuilder.build(document, indexConfig);
            Term term = new Term(primaryName, StringUtil.conver2String(document.get(primaryName)));
            long seqNo = this.indexWriter.updateDocument(term, insert);
            this.indexWriter.commit();
            return seqNo >= 0 ? 1 : 0;
        } catch (IOException ex) {
            throw new LuceneException("MemoryIndex insert/update error", ex);
        } catch (InterruptedException ex) {
            throw new LuceneException("MemoryIndex is reloading, cannot insert/update.", ex);
        } finally {
            if (lock) {
                reloadLock.readLock().unlock();
            }
        }
    }

    /**
     * 插入单个文档到内存索引，必须给出主键字段<br/>
     * 注意：是整体覆盖更新，不是只更新某些字段!!!<br/>
     * 每次必须给出全量字段，只更新某些字段现在还没有做，等有空了做
     *
     * @param id
     * @param document
     * @throws IOException
     */
    public int insertUpdate(String id, Document document) throws LuceneException {

        if (StringUtil.isEmpty(id) || document == null) {
            return 0;
        }

        boolean lock = false;
        try {
            lock = reloadLock.readLock().tryLock(10, TimeUnit.SECONDS);
            if (!lock) {
                throw new LuceneException("MemoryIndex is reloading, cannot insert/update.");
            }

            String primaryName = indexConfig.getPrimaryField().getName();
            Term term = new Term(primaryName, id);
            long seqNo = this.indexWriter.updateDocument(term, document);
            this.indexWriter.commit();
            return seqNo >= 0 ? 1 : 0;
        } catch (IOException ex) {
            throw new LuceneException("MemoryIndex insert/update error", ex);
        } catch (InterruptedException ex) {
            throw new LuceneException("MemoryIndex is reloading, cannot insert/update.", ex);
        } finally {
            if (lock) {
                reloadLock.readLock().unlock();
            }
        }
    }


    /**
     * 批量插入文档到内存索引，必须给出主键字段<br/>
     * 注意：是整体覆盖更新，不是只更新某些字段!!!<br/>
     * 每次必须给出全量字段，只更新某些字段现在还没有做，等有空了做
     *
     * @param documents
     * @throws IOException
     */
    public int batchInsertUpdate(List<Map<String, Object>> documents) throws LuceneException {

        if (documents == null || documents.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        boolean lock = false;
        try {
            lock = reloadLock.readLock().tryLock(10, TimeUnit.SECONDS);
            if (!lock) {
                throw new LuceneException("MemoryIndex is reloading, cannot insert/update.");
            }
            String primaryName = indexConfig.getPrimaryField().getName();

            for(Map<String, Object> document : documents) {
                if (!document.containsKey(primaryName)) {
                    continue;
                }
                Document insert = DocumentBuilder.build(document, indexConfig);
                Term term = new Term(primaryName, StringUtil.conver2String(document.get(primaryName)));
                long seqNo = this.indexWriter.updateDocument(term, insert);
                if(seqNo >= 0) {
                    successCount++;
                }
            }
            this.indexWriter.commit();
        } catch (IOException ex) {
            throw new LuceneException("MemoryIndex insert/update error", ex);
        } catch (InterruptedException ex) {
            throw new LuceneException("MemoryIndex is reloading, cannot insert/update.", ex);
        } finally {
            if (lock) {
                reloadLock.readLock().unlock();
            }
        }
        return successCount;
    }


    /**
     * 批量插入文档到内存索引，必须给出主键字段(key为主键, value为文档)<br/>
     * 注意：是整体覆盖更新，不是只更新某些字段!!!<br/>
     * 每次必须给出全量字段，只更新某些字段现在还没有做，等有空了做
     *
     * @param documentMap
     * @throws IOException
     */
    public int batchInsertUpdate(Map<String, Document> documentMap) throws LuceneException {

        if (documentMap == null || documentMap.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        boolean lock = false;
        try {
            lock = reloadLock.readLock().tryLock(10, TimeUnit.SECONDS);
            if (!lock) {
                throw new LuceneException("MemoryIndex is reloading, cannot insert/update.");
            }
            String primaryName = indexConfig.getPrimaryField().getName();

            for(Map.Entry<String, Document> entry : documentMap.entrySet()) {
                String id = entry.getKey();
                Document insert = entry.getValue();
                if(insert == null) {
                    continue;
                }
                Term term = new Term(primaryName, id);
                long seqNo = this.indexWriter.updateDocument(term, insert);
                if(seqNo >= 0) {
                    successCount++;
                }
            }
            this.indexWriter.commit();
        } catch (IOException ex) {
            throw new LuceneException("MemoryIndex insert/update error", ex);
        } catch (InterruptedException ex) {
            throw new LuceneException("MemoryIndex is reloading, cannot insert/update.", ex);
        } finally {
            if (lock) {
                reloadLock.readLock().unlock();
            }
        }
        return successCount;
    }


    /**
     * 根据主键删除文档
     *
     * @param id
     * @throws IOException
     */
    public void delete(String id) throws LuceneException {
        if (StringUtil.isEmpty(id)) {
            throw new IllegalArgumentException("MemoryIndex delete must has primary value.");
        }

        boolean lock = false;
        try {
            lock = reloadLock.readLock().tryLock(10, TimeUnit.SECONDS);
            if (!lock) {
                throw new LuceneException("MemoryIndex is reloading, cannot delete.");
            }

            IndexFieldMapping primaryConf = indexConfig.getPrimaryField();
            String primaryName = primaryConf.getName();
            Term term = new Term(primaryName, id);
            this.indexWriter.deleteDocuments(term);
            this.indexWriter.commit();
        } catch (IOException e) {
            throw new LuceneException("MemoryIndex delete document error", e);
        } catch (InterruptedException e) {
            throw new LuceneException("MemoryIndex is reloading, cannot delete.");
        } finally {
            if (lock) {
                reloadLock.readLock().unlock();
            }
        }
    }

    /**
     * 获取当前索引的文档总数
     *
     * @return
     */
    public int count() {
        IndexSearcher searcher = null;
        boolean lock = false;
        try {
            lock = reloadLock.readLock().tryLock(10, TimeUnit.SECONDS);
            if (!lock) {
                throw new LuceneException("MemoryIndex is reloading, cannot query.");
            }
            searcher = this.searcherManager.acquire();
            return searcher.getIndexReader().numDocs();
        } catch (IOException ex) {
            throw new LuceneException("MemoryIndex count error", ex);
        } catch (InterruptedException e) {
            throw new LuceneException("MemoryIndex is reloading, cannot query.", e);
        } finally {
            if (lock) {
                reloadLock.readLock().unlock();
            }
        }
    }

    /**
     * 如果IndexWriter有变更，则更新IndexSearcher，如果没有，保持现状
     *
     * @throws IOException
     */
    public void maybeRefresh() throws LuceneException {
        boolean lock = false;
        try {
            lock = reloadLock.readLock().tryLock(10, TimeUnit.SECONDS);
            if (!lock) {
                throw new LuceneException("MemoryIndex is reloading, cannot query.");
            }
            this.searcherManager.maybeRefresh();
        } catch (IOException ex) {
            throw new LuceneException("MemoryIndex maybeRefresh error", ex);
        } catch (InterruptedException e) {
            throw new LuceneException("MemoryIndex is reloading, cannot query.", e);
        } finally {
            if (lock) {
                reloadLock.readLock().unlock();
            }
        }
    }

    /**
     * 重新加载数据，加载完成后替换原始数据时，会阻塞全部操作
     */
    public void reloadData() throws LuceneException {
        try {
            // 先读取数据
            List<Document> documents = null;
            if (this.indexDataLoader != null) {
                documents = this.indexDataLoader.load(this.indexConfig);
            }

            // 写锁更新
            boolean lock = reloadLock.writeLock().tryLock(60, TimeUnit.SECONDS);
            if (lock) {
                Directory directoryLocal = new ByteBuffersDirectory();
                Analyzer defAnalyzer = new KeywordAnalyzer();
                Map<String, Analyzer> fieldAnalyzers = this.indexConfig.getFieldAnalyzers();
                PerFieldAnalyzerWrapper analyzerWrapper = new PerFieldAnalyzerWrapper(defAnalyzer, fieldAnalyzers);
                IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzerWrapper);
                indexWriterConfig.setSimilarity(new BM25Similarity());
                IndexWriter indexWriterLocal = new IndexWriter(directoryLocal, indexWriterConfig);

                if (documents != null) {
                    for (Document document : documents) {
                        indexWriterLocal.addDocument(document);
                    }
                    indexWriterLocal.commit();
                }
                SearcherManager searcherManagerLocal = new SearcherManager(indexWriterLocal, new CustomSearcherFactory());

                this.indexWriter.close();
                this.searcherManager.close();
                this.directory = directoryLocal;
                this.indexWriter = indexWriterLocal;
                this.searcherManager = searcherManagerLocal;
            } else {
                throw new LuceneException("MemoryIndex reload lock error");
            }
        } catch (IOException ex) {
            throw new LuceneException("MemoryIndex reload error", ex);
        } catch (InterruptedException ex) {
            throw new LuceneException("MemoryIndex reload error, cannot lock", ex);
        } finally {
            reloadLock.writeLock().unlock();
        }
    }


    /**
     * 分组查询
     * @param request
     * @return
     * @throws QueryException
     */
    public SearchResponse search(SearchRequest request) throws QueryException {

        IndexSearcher searcher = null;
        boolean lock = false;
        try {
            lock = reloadLock.readLock().tryLock(5, TimeUnit.SECONDS);
            if (!lock) {
                throw new QueryException("MemoryIndex is reloading, cannot query.");
            }

            searcher = this.searcherManager.acquire();

            int maxDoc = Math.max(1, searcher.getIndexReader().maxDoc());
            ScoreDoc after = request.getLastDoc();
            if (after != null) {
                if(CollectionUtils.isNotEmpty(request.getSorts()) && !(after instanceof FieldDoc)) {
                    throw new QueryException("When sorting query, after must be a FieldDoc; got " + after);
                }
                if (after.doc >= maxDoc) {
                    throw new QueryException("after.doc exceeds the number of documents in the reader: after.doc="
                            + after.doc + " maxDoc=" + maxDoc);
                }
            }

            // 初始化分页
            int pageNum = 1;
            int pageSize = 10;
            PageArg pageArg = request.getPageArg();
            if (pageArg != null) {
                pageNum = pageArg.getPageNum() < 1 ? 1 : pageArg.getPageNum();
                pageSize = pageArg.getPageSize() < 0 ? 10 : pageArg.getPageSize();
            }
            if(pageNum > 1 && request.getFilter() != null) {
                throw new QueryException("Paging query disabling result filtering");
            }
            if(pageSize <= 0 && CollectionUtils.isEmpty(request.getAggregators())) {
                return new SearchResponse();
            }

            // 构建查询
            Query query = QueryBuilder.createQuery(request.getCriteriaList(), indexConfig, null);
            // 构造排序字段
            Sort sort = SortBuilder.buildSort(request.getSorts(), indexConfig);

            // 构建查询上下文
            SearchContext searchContext = new SearchContext();
            searchContext.setRequest(request);
            searchContext.setQuery(query);
            searchContext.setIndexConfig(indexConfig);
            searchContext.setSearcher(searcher);

            int numHits = pageSize;
            int start = 0;
            int end = pageSize;
            if(after == null) {
                start = (pageNum - 1) * pageSize;
                end = pageNum * pageSize;
                // 浅分页时，需要获取到当前页之前（包括）的所有数据
                numHits = pageNum * pageSize;
            }

            final int cappedNumHits = Math.min(numHits, maxDoc);
            // 执行查询
            TopDocsCollector topCollector = null;
            if(cappedNumHits > 0) {
                if(sort == null) {
                    topCollector = TopScoreDocCollector.create(cappedNumHits, after, TOTAL_HITS_THRESHOLD);
                } else {
                    final Sort rewrittenSort = sort.rewrite(searcher);
                    searchContext.setSort(rewrittenSort);
                    topCollector = TopFieldCollector.create(rewrittenSort, cappedNumHits, (FieldDoc) after, TOTAL_HITS_THRESHOLD);
                }
                searchContext.setTopCollector(topCollector);
            }

            Collector collector;
            List<Aggregator> aggregators = request.getAggregators();
            if(CommonUtil.isNotEmpty(aggregators)) {
                // 预处理分组收集器
                preAggregator(aggregators, searchContext);
                searchContext.setAggregators(aggregators);

                List<Collector> subCollectors = new ArrayList<> ();
                if(topCollector != null) {
                    subCollectors.add(topCollector);
                }
                subCollectors.addAll(aggregators);
                collector = MultiCollector.wrap(subCollectors);
            } else {
                collector = topCollector;
            }

            // 执行搜索
            searcher.search(query, collector);

            // 构造hits
            SearchResponse response = new SearchResponse();
            SearchHits hits;
            if(topCollector != null) {
                TopDocs topDocs = topCollector.topDocs();
                // 构造top响应
                hits = SearchHitsBuilder.build(request, topDocs, this.indexConfig, searcher, start, end);
            } else {
                hits = new SearchHits();
            }
            response.setHits(hits);

            // 构造分组aggs
            if(CommonUtil.isNotEmpty(aggregators)) {
                // 后置处理分组收集器
                postAggregator(aggregators, searchContext);
                // 收集aggs结果
                response.setAggregations(buildAggergator(aggregators));
            }

            if(request.isExplain() && CommonUtil.isNotEmpty(hits.getDocuments())) {
                List<Explanation> explanations = explain(query, hits);
                response.setExplanations(explanations);
            }

            return response;
        } catch (IOException ex) {
            throw new QueryException("MemoryIndex query error", ex);
        } catch (InterruptedException e) {
            throw new QueryException("MemoryIndex is reloading, cannot query.", e);
        } finally {
            if (lock) {
                reloadLock.readLock().unlock();
            }
            if (searcher != null) {
                try {
                    this.searcherManager.release(searcher);
                } catch (IOException e) {
                    logger.error("MemoryIndex release IndexSearcher error:{}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 查看执行计划
     * @param query
     * @param hits
     * @return
     * @throws QueryException
     */
    public List<Explanation> explain(Query query, SearchHits hits) throws QueryException {

        IndexSearcher searcher = null;
        boolean lock = false;
        try {
            lock = reloadLock.readLock().tryLock(5, TimeUnit.SECONDS);
            if (!lock) {
                throw new QueryException("MemoryIndex is reloading, cannot query.");
            }

            searcher = this.searcherManager.acquire();

            List<Explanation> explanationList = new ArrayList<>();
            for(Map<String, Object> document : hits.getDocuments()) {
                int docId = Integer.parseInt(StringUtil.conver2String(document.get(Constants._ID)));
                // 执行计划
                Explanation explanation = searcher.explain(query, docId);
                explanationList.add(explanation);
            }


            return explanationList;
        } catch (IOException ex) {
            throw new QueryException("MemoryIndex explain error", ex);
        } catch (InterruptedException e) {
            throw new QueryException("MemoryIndex is reloading, cannot query.", e);
        } finally {
            if (lock) {
                reloadLock.readLock().unlock();
            }
            if (searcher != null) {
                try {
                    this.searcherManager.release(searcher);
                } catch (IOException e) {
                    logger.error("MemoryIndex release IndexSearcher error:{}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 查询前预处理分组收集器
     */
    private void preAggregator(List<Aggregator> aggregators, SearchContext searchContext) throws IOException {
        for(Aggregator aggregator : aggregators) {
            if(aggregator.isCollected()) {
                throw new QueryException("Aggregator is collected, please create a new aggregator");
            }
            aggregator.preCollection(searchContext);
            if(CommonUtil.isNotEmpty(aggregator.subAggregator())) {
                preAggregator(aggregator.subAggregator(), searchContext);
            }
        }
    }

    /**
     * 查询后、构造分组结果前处理分组收集器
     */
    private void postAggregator(List<Aggregator> aggregators, SearchContext searchContext) throws IOException {
        for(Aggregator aggregator : aggregators) {
            if(aggregator.isCollected()) {
                throw new QueryException("Aggregator is collected, please create a new aggregator");
            }
            aggregator.postCollection(searchContext);
            if(CommonUtil.isNotEmpty(aggregator.subAggregator())) {
                postAggregator(aggregator.subAggregator(), searchContext);
            }
        }
    }

    /**
     * 构造分组结果
     */
    private List<InternalAggregation> buildAggergator(List<Aggregator> aggregators) throws IOException {
        List<InternalAggregation> aggregations = new ArrayList<>(aggregators.size());
        for(Aggregator aggregator : aggregators) {
            if(aggregator.isCollected()) {
                throw new QueryException("Aggregator is collected, please create a new aggregator");
            }
            aggregations.add(aggregator.buildAggregation(0));
        }
        return aggregations;
    }

    public void close() throws LuceneException {
        try {
            this.schedule.shutdownNow();
            this.indexWriter.close();
            this.searcherManager.close();
        } catch (IOException ex) {
            throw new LuceneException("MemoryIndex close error", ex);
        }
    }

}
