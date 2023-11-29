package org.lccy.lucene.memory.index;

import org.lccy.lucene.memory.exception.LuceneException;
import org.lccy.lucene.memory.exception.QueryException;
import org.lccy.lucene.memory.index.config.IndexConfig;
import org.lccy.lucene.memory.index.dto.IndexFieldDto;
import org.lccy.lucene.memory.index.dto.IndexSettingDto;
import org.lccy.lucene.memory.loader.IndexDataLoader;
import org.lccy.lucene.memory.search.CustomSearcherFactory;
import org.lccy.lucene.memory.search.SearchRequest;
import org.lccy.lucene.memory.search.SearchResponse;
import org.lccy.lucene.util.StringUtil;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基于lucene的内存倒序索引，支持插入、更新、删除、查询操作<br/>
 * 注意：保存的数据量不宜过大，数据量过多，考虑使用elasticsearch、solr等可靠的基于文件系统的方案<br/>
 *
 * @Date: 2023/11/21 09:08 <br>
 * @author: liuchen11
 */
public class MemoryIndex {

    private static final Logger logger = LoggerFactory.getLogger(MemoryIndex.class);

    private Directory directory;
    private IndexConfig indexConfig;
    private IndexWriter indexWriter;
    private SearcherManager searcherManager;
    private IndexDataLoader indexDataLoader;
    // reload时禁止其他操作（插入、更新、删除、查询）
    private final ReadWriteLock reloadLock = new ReentrantReadWriteLock();
    private Timer timer;

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
        this.timer = new Timer();
        IndexSettingDto indexSetting = this.indexConfig.getIndexSetting();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                maybeRefresh();
            }
        }, 0, indexSetting.getRefreshInterval());
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
     * 插入单个文档到内存索引，必须给出主键字段
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

            String id = StringUtil.conver2String(document.get(primaryName));
            Document insert = new Document();
            for (Map.Entry<String, Object> entry : document.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                IndexFieldDto fieldConf = indexConfig.getFieldConfig(fieldName);
                fieldConf.getType().convertField(insert, fieldName, fieldConf, value);
            }
            Term term = new Term(primaryName, id);
            this.indexWriter.updateDocument(term, insert);
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
        return 1;
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

            IndexFieldDto primaryConf = indexConfig.getPrimaryField();
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
     * 查询数据
     *
     * @param request
     * @return
     * @throws IOException
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
            // 构建查询
            Query query = request.createQuery(request.getCriteriaList(), indexConfig);
            // 执行查询
            TopDocs topDocs = searcher.search(query, 10);

            SearchResponse response = new SearchResponse();
            response.setSize(topDocs.totalHits.value);
            List<Map> dataList = new ArrayList<>();
            float maxScore = 0.0f;
            // 处理查询结果
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Map<String, Object> line = new HashMap<>();

                Document resultDoc = searcher.doc(scoreDoc.doc);
                float score = scoreDoc.score;
                if (maxScore < score) {
                    maxScore = score;
                }
                line.put("_score", score);
                for (Iterator<IndexableField> it = resultDoc.iterator(); it.hasNext(); ) {
                    IndexableField field = it.next();
                    String fieldName = field.name();
                    IndexFieldDto fieldConf = indexConfig.getFieldConfig(fieldName);
                    if (line.containsKey(fieldName)) {
                        Object exists = line.get(fieldName);
                        if (exists instanceof List) {
                            ((List) exists).add(fieldConf.convertStoreValue(field));
                        } else {
                            List t = new ArrayList();
                            t.add(fieldConf.convertStoreValue(field));
                            line.put(fieldName, t);
                        }
                    } else {
                        line.put(fieldName, fieldConf.convertStoreValue(field));
                    }
                }
                dataList.add(line);
            }
            response.setMaxScore(maxScore);
            response.setDocument(dataList);
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
     * 查询数据
     *
     * @param request
     * @return
     * @throws IOException
     */
    public SearchResponse search(SearchRequest request, Collector collector) throws QueryException {
        IndexSearcher searcher = null;
        boolean lock = false;
        try {
            lock = reloadLock.readLock().tryLock(5, TimeUnit.SECONDS);
            if (!lock) {
                throw new QueryException("MemoryIndex is reloading, cannot query.");
            }

            searcher = this.searcherManager.acquire();
            // 构建查询
            Query query = request.createQuery(request.getCriteriaList(), indexConfig);
            // 执行查询
            searcher.search(query, collector);

            SearchResponse response = new SearchResponse();
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
    public void reloadData() {
        try {
            // 写锁更新
            boolean lock = reloadLock.writeLock().tryLock(30, TimeUnit.SECONDS);

            if (lock) {
                Directory directoryLocal = new ByteBuffersDirectory();
                Analyzer defAnalyzer = new KeywordAnalyzer();
                Map<String, Analyzer> fieldAnalyzers = this.indexConfig.getFieldAnalyzers();
                PerFieldAnalyzerWrapper analyzerWrapper = new PerFieldAnalyzerWrapper(defAnalyzer, fieldAnalyzers);
                IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzerWrapper);
                indexWriterConfig.setSimilarity(new BM25Similarity());
                IndexWriter indexWriterLocal = new IndexWriter(directoryLocal, indexWriterConfig);

                if (this.indexDataLoader != null) {
                    List<Document> documents = this.indexDataLoader.load(this.indexConfig);
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
            }
        } catch (IOException ex) {
            throw new LuceneException("MemoryIndex reload error", ex);
        } catch (InterruptedException ex) {
            throw new LuceneException("MemoryIndex reload error, cannot lock", ex);
        } finally {
            reloadLock.writeLock().unlock();
        }
    }


}
