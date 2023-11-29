package org.lccy.lucene.memory.index.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/11/21 17:44 <br>
 * @author: liuchen11
 */
@Getter
@Setter
public class IndexDto {
    private List<IndexFieldDto> mapping;
    private IndexSettingDto setting;
}
