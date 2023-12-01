package org.lccy.lucene.memory.aggs.collector.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.lucene.search.Query;


/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/12/16 17:21 <br>
 * @author: liuchen11
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KeyedFilter {

    private String key;

    private Query query;
}
