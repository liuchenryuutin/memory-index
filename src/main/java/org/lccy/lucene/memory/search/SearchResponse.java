package org.lccy.lucene.memory.search;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/11/23 10:06 <br>
 * @author: liuchen11
 */
@Getter
@Setter
public class SearchResponse {

    private long size = 0;
    private float maxScore = 0.0f;
    private List<Map> document;

}
