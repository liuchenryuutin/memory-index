package org.lccy.lucene.memory.search;

import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.search.ScoreDoc;
import org.lccy.lucene.memory.aggs.collector.Aggregator;

import java.util.List;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/11/22 17:08 <br>
 * @author: liuchen11
 */
@Getter
@Setter
public class SearchRequest {
    // 分页参数
    private PageArg pageArg;
    // 查询条件
    private List<SearchCriteria> criteriaList;
    // 排序字段
    private List<SortFieldInfo> sorts;
    // 深分页时使用
    private ScoreDoc lastDoc;
    // 添加排序字段后是否继续评分
    private boolean doDocScores;
    // 包含字段
    private List<String> include;
    // 排除字段
    private List<String> exclude;
    // 在查询结果上继续过滤结果集，比如查询指定数据、去重等，但是此时总条数是不精确的，分页查询时禁用，适合查询size很大时，取topN的数据
    private SearchResultFilter filter;
    // 分组查询条件
    private List<Aggregator> aggregators;
    // 执行计划
    private boolean explain = false;

    public SearchRequest() {
    }

    public SearchRequest(PageArg pageArg) {
        this();
        this.pageArg = pageArg;
    }

    public SearchRequest(PageArg pageArg, List<SearchCriteria> criteriaList) {
        this(pageArg);
        this.criteriaList = criteriaList;
    }

}
