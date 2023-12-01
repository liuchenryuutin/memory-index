package org.lccy.lucene.memory.aggs.collector.metrics;

import org.apache.lucene.search.Sort;
import org.lccy.lucene.memory.builder.SortBuilder;
import org.lccy.lucene.memory.index.config.IndexConfig;
import org.lccy.lucene.memory.search.SortFieldInfo;
import org.lccy.lucene.memory.util.CollectionUtils;

import java.util.List;

/**
 * top_hits分组配置信息
 *
 * @Date: 2023/12/13 09:52 <br>
 * @author: liuchen11
 */
public class TopHitsAggsConfig {

    private int from;
    private int size;
    private boolean explain;
    private boolean trackScores;
    // 排序字段配置
    private List<SortFieldInfo> sorts;
    private List<String> include;
    private List<String> exclude;
    private Sort sort;

    /**
     * 获取排序并缓存
     *
     * @param indexConfig
     * @return
     */
    public Sort getSort(IndexConfig indexConfig) {
        if (sort == null) {
            if (CollectionUtils.isEmpty(sorts)) {
                return null;
            } else {
                sort = SortBuilder.buildSort(sorts, indexConfig);
            }
        }
        return sort;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isExplain() {
        return explain;
    }

    public void setExplain(boolean explain) {
        this.explain = explain;
    }

    public boolean isTrackScores() {
        return trackScores;
    }

    public void setTrackScores(boolean trackScores) {
        this.trackScores = trackScores;
    }

    public List<SortFieldInfo> getSorts() {
        return sorts;
    }

    public void setSorts(List<SortFieldInfo> sorts) {
        this.sorts = sorts;
    }

    public List<String> getInclude() {
        return include;
    }

    public void setInclude(List<String> include) {
        this.include = include;
    }

    public List<String> getExclude() {
        return exclude;
    }

    public void setExclude(List<String> exclude) {
        this.exclude = exclude;
    }
}
