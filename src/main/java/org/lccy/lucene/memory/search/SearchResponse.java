package org.lccy.lucene.memory.search;

import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.search.Explanation;
import org.lccy.lucene.memory.aggs.collector.aggregation.InternalAggregation;

import java.util.List;

/**
 * 查询返回结果
 *
 * @Date: 2023/11/23 10:06 <br>
 * @author: liuchen11
 */
@Getter
@Setter
public class SearchResponse {

    // hit信息
    private SearchHits hits;
    // 分组信息
    private List<InternalAggregation> aggregations;
    // 执行计划
    private List<Explanation> explanations;
}
