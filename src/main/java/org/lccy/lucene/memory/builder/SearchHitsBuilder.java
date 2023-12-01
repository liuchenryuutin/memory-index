package org.lccy.lucene.memory.builder;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.*;
import org.lccy.lucene.memory.aggs.collector.metrics.TopHitsAggsConfig;
import org.lccy.lucene.memory.constants.Constants;
import org.lccy.lucene.memory.index.config.IndexConfig;
import org.lccy.lucene.memory.index.mapping.IndexFieldMapping;
import org.lccy.lucene.memory.search.SearchContext;
import org.lccy.lucene.memory.search.SearchHits;
import org.lccy.lucene.memory.search.SearchRequest;
import org.lccy.lucene.memory.search.SearchResultFilter;
import org.lccy.lucene.memory.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 查询结果SearchHits构造
 *
 * @Date: 2023/12/15 23:28 <br>
 * @author: liuchen11
 */
public final class SearchHitsBuilder {

    private SearchHitsBuilder() {}


    /**
     * 根据start、end下标位置获取数据
     *
     * @param topDocs      查询返回top结果
     * @param idxConf      索引配置
     * @param searcher     索引查询器
     * @param start        开始下标（包含）
     * @param end          结束下标（不包含）
     */
    public static SearchHits build(SearchRequest request, TopDocs topDocs, IndexConfig idxConf, IndexSearcher searcher, int start, int end) throws IOException {
        SearchHits result = new SearchHits();
        List<Map> ducuments = new ArrayList<>();
        result.setDocuments(ducuments);
        long total = topDocs.totalHits.value;
        // 处理查询结果
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        if (scoreDocs.length == 0 || start >= scoreDocs.length) {
            return result;
        }

        int sortScoreIdx = -1;
        if(topDocs instanceof TopFieldDocs) {
            SortField[] sortFields = ((TopFieldDocs) topDocs).fields;
            for (int i = 0; i < sortFields.length; i++) {
                if (sortFields[i] == SortField.FIELD_SCORE) {
                    sortScoreIdx = i;
                    break;
                }
            }
            result.setSortFields(sortFields);
        }

        float maxScore = 0.0f;
        int i;
        List<String> include = request.getInclude();
        Map<String, Integer> exclude = CollectionUtils.isEmpty(request.getExclude()) ? null : request.getExclude().stream().collect(Collectors.toMap(x -> x, x -> 1));
        SearchResultFilter resultFilter = request.getFilter();
        for (i = start; i < end && i < scoreDocs.length; i++) {
            ScoreDoc scoreDoc = scoreDocs[i];
            Document document = searcher.doc(scoreDoc.doc);

            // 在查询结果上继续过滤结果集，比如查询指定数据、去重等，但是此时总条数是不精确的，分页查询禁用，适合查询size很大时，取topN的数据
            if(resultFilter != null && resultFilter.filter(scoreDoc, document)) {
                total--;
                continue;
            }

            Map<String, Object> line = new HashMap<>();
            float score = scoreDoc.score;
            if (Float.isNaN(score) && sortScoreIdx >= 0 && scoreDoc instanceof FieldDoc) {
                FieldDoc fieldDoc = (FieldDoc) scoreDoc;
                Object[] otherFields = fieldDoc.fields;
                if(sortScoreIdx < otherFields.length) {
                    score = ((Number) otherFields[sortScoreIdx]).floatValue();
                }

            }
            line.put(Constants._ID, scoreDoc.doc);
            line.put(Constants._SCORE, score);
            if (maxScore < score) {
                maxScore = score;
            }

            if (CollectionUtils.isNotEmpty(include)) {
                for (String fieldName : include) {
                    IndexFieldMapping fieldConf = idxConf.getFieldConfig(fieldName);
                    IndexableField[] fields = document.getFields(fieldName);
                    if (fields.length == 0) {
                        continue;
                    } else if (fields.length == 1) {
                        line.put(fieldName, fieldConf.convertStoreValue(fields[0]));
                    } else {
                        List values = new ArrayList();
                        for (IndexableField field : fields) {
                            values.add(fieldConf.convertStoreValue(field));
                        }
                        line.put(fieldName, values);
                    }
                }
            } else {
                for (Iterator<IndexableField> it = document.iterator(); it.hasNext(); ) {
                    IndexableField field = it.next();
                    String fieldName = field.name();
                    if (exclude != null && exclude.containsKey(fieldName)) {
                        continue;
                    }
                    IndexFieldMapping fieldConf = idxConf.getFieldConfig(fieldName);
                    if (line.containsKey(fieldName)) {
                        Object exists = line.get(fieldName);
                        if (exists instanceof List) {
                            ((List) exists).add(fieldConf.convertStoreValue(field));
                        } else {
                            List t = new ArrayList();
                            t.add(exists);
                            t.add(fieldConf.convertStoreValue(field));
                            line.put(fieldName, t);
                        }
                    } else {
                        line.put(fieldName, fieldConf.convertStoreValue(field));
                    }
                }
            }

            ducuments.add(line);
        }
        // 获取到start-end区间全部记录时（代表当前页是满的），记录当前页的最后一个对象，方便之后进行深度分页搜索
        if (i == end) {
            result.setLastDoc(scoreDocs[scoreDocs.length - 1]);
        }
        result.setMaxScore(maxScore);
        result.setTotal(total);
        return result;
    }

    /**
     * top_hits分组查询，根据topDocs获取数据
     *
     * @param topDocs       查询返回top结果
     * @param searchContext 查询上下文
     * @param topHitsAggsConfig top_hits配置
     */
    public static SearchHits buildHits(TopDocs topDocs, SearchContext searchContext, TopHitsAggsConfig topHitsAggsConfig) throws IOException {
        SearchHits result = new SearchHits();
        List<Map> dataList = new ArrayList<>();
        result.setDocuments(dataList);
        long total = topDocs.totalHits.value;
        int start = topHitsAggsConfig.getFrom();
        int end = start + topHitsAggsConfig.getSize();
        // 处理查询结果
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        if (scoreDocs.length == 0 || start >= scoreDocs.length) {
            return result;
        }
        int sortScoreIdx = -1;
        if (topDocs instanceof TopFieldDocs) {
            SortField[] sortFields = ((TopFieldDocs) topDocs).fields;
            for (int i = 0; i < sortFields.length; i++) {
                if (sortFields[i] == SortField.FIELD_SCORE) {
                    sortScoreIdx = i;
                    break;
                }
            }
            result.setSortFields(sortFields);
        }
        float maxScore = 0.0f;

        List<String> include = topHitsAggsConfig.getInclude();
        Map<String, Integer> exclude = CollectionUtils.isEmpty(topHitsAggsConfig.getExclude()) ? null : topHitsAggsConfig.getExclude().stream().collect(java.util.stream.Collectors.toMap(x -> x, x -> 1));
        final IndexConfig indexConfig = searchContext.getIndexConfig();
        final IndexSearcher indexSearcher = searchContext.getSearcher();
        for (int i = start; i < end && i < scoreDocs.length; i++) {
            ScoreDoc scoreDoc = scoreDocs[i];
            Document document = indexSearcher.doc(scoreDoc.doc);

            Map<String, Object> line = new HashMap<>();
            float score = scoreDoc.score;
            if (Float.isNaN(score) && sortScoreIdx >= 0 && scoreDoc instanceof FieldDoc) {
                FieldDoc fieldDoc = (FieldDoc) scoreDoc;
                Object[] otherFields = fieldDoc.fields;
                if (sortScoreIdx < otherFields.length) {
                    score = ((Number) otherFields[sortScoreIdx]).floatValue();
                }
            }
            line.put(Constants._ID, scoreDoc.doc);
            line.put(Constants._SCORE, score);
            if (maxScore < score) {
                maxScore = score;
            }

            if (CollectionUtils.isNotEmpty(include)) {
                for (String fieldName : include) {
                    IndexFieldMapping fieldConf = indexConfig.getFieldConfig(fieldName);
                    IndexableField[] fields = document.getFields(fieldName);
                    if (fields.length == 0) {
                        continue;
                    } else if (fields.length == 1) {
                        line.put(fieldName, fieldConf.convertStoreValue(fields[0]));
                    } else {
                        List values = new ArrayList();
                        for (IndexableField field : fields) {
                            values.add(fieldConf.convertStoreValue(field));
                        }
                        line.put(fieldName, values);
                    }
                }
            } else {
                for (Iterator<IndexableField> it = document.iterator(); it.hasNext(); ) {
                    IndexableField field = it.next();
                    String fieldName = field.name();
                    if (exclude != null && exclude.containsKey(fieldName)) {
                        continue;
                    }
                    IndexFieldMapping fieldConf = indexConfig.getFieldConfig(fieldName);
                    if (line.containsKey(fieldName)) {
                        Object exists = line.get(fieldName);
                        if (exists instanceof List) {
                            ((List) exists).add(fieldConf.convertStoreValue(field));
                        } else {
                            List t = new ArrayList();
                            t.add(exists);
                            t.add(fieldConf.convertStoreValue(field));
                            line.put(fieldName, t);
                        }
                    } else {
                        line.put(fieldName, fieldConf.convertStoreValue(field));
                    }
                }
            }

            dataList.add(line);
        }
        result.setMaxScore(maxScore);
        result.setTotal(total);
        return result;
    }
}
