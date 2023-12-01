package org.lccy.lucene.memory.index.mapping;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2024/01/30 16:08 <br>
 * @author: liuchen11
 */
@Getter
@Setter
public class NestedMapping {

    // 嵌套字段配置
    private List<IndexFieldMapping> nestedFields;

}
