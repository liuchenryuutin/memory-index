package org.lccy.lucene.memory.index.mapping;

import lombok.Getter;
import lombok.Setter;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/11/21 17:25 <br>
 * @author: liuchen11
 */
@Getter
@Setter
public class IndexSettingMapping {

    // 刷新时间，默认30s
    private long refreshInterval = 30000l;

    private boolean dynamicsMapping = true;
}
