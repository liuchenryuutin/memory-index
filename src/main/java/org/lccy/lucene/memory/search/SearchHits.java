package org.lccy.lucene.memory.search;

import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;

import java.util.List;
import java.util.Map;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/12/15 22:34 <br>
 * @author: liuchen11
 */
@Getter
@Setter
public class SearchHits {

    private List<Map> documents;

    private long total = 0;

    private float maxScore = Float.NaN;

    private ScoreDoc lastDoc;

    private SortField[] sortFields;
}
