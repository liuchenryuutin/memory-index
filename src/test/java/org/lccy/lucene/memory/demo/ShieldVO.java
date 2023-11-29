package org.lccy.lucene.memory.demo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/11/25 20:02 <br>
 * @author: liuchen11
 */
@Setter
@Getter
public class ShieldVO implements Serializable {
    private static final long serialVersionUID = 8013490778359361839L;

    private String provCode;
    private Date startTime;
    private Date endTime;
    private String version;
}
