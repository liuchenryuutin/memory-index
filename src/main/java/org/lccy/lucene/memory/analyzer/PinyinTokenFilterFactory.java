package org.lccy.lucene.memory.analyzer;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.elasticsearch.analysis.PinyinConfig;
import org.elasticsearch.index.analysis.PinyinTokenFilter;

import java.util.HashMap;

/**
 * 拼音TokenFilter工厂类
 *
 * @Date: 2023/11/28 20:17 <br>
 * @author: liuchen11
 */
public class PinyinTokenFilterFactory extends TokenFilterFactory {

    private PinyinConfig config;

    protected PinyinTokenFilterFactory(PinyinConfig config) {
        super(new HashMap<>());
        this.config = config;
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new PinyinTokenFilter(tokenStream, config);
    }

}
