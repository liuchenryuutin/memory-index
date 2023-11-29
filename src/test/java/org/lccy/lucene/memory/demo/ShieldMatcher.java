package org.lccy.lucene.memory.demo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.lccy.lucene.memory.query.AbstractMatcher;
import org.lccy.lucene.util.CollectionUtils;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Explanation;

import java.util.List;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/11/26 16:57 <br>
 * @author: liuchen11
 */
public class ShieldMatcher extends AbstractMatcher<ShieldVO> {

    @Override
    public boolean match(String json, ShieldVO term) {
        if (json.startsWith("[")) {
            List<ShieldVO> datas = JSON.parseObject(json, new TypeReference<List<ShieldVO>>() {
            });
            if (!CollectionUtils.isEmpty(datas)) {
                for (ShieldVO data : datas) {
                    if (intersect(data, term)) {
                        return true;
                    }
                }
            }
        } else {
            ShieldVO data = JSON.parseObject(json, ShieldVO.class);
            if (intersect(data, term)) {
                return true;
            }
        }
        return false;
    }

    private boolean intersect(ShieldVO data, ShieldVO term) {
        if (data.getProvCode().equals(term.getProvCode())) {
            if (data.getStartTime() == null && data.getEndTime() == null) {
                return true;
            } else {
                if (data.getEndTime() != null && term.getStartTime().compareTo(data.getEndTime()) <= 0) {
                    return true;
                }
                if (data.getStartTime() != null && term.getEndTime().compareTo(data.getStartTime()) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public float doScore(LeafReaderContext context, ShieldVO term, float boost) {
        return 1;
    }

    @Override
    public Explanation doExplain(LeafReaderContext context, int doc) {
        return Explanation.match(1, "Shield json filter match score 1.");
    }
}
