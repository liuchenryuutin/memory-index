package org.lccy.lucene.memory.analyzer;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

import java.util.HashMap;

/**
 * IK分词器工厂类
 *
 * @Date: 2023/11/28 19:13 <br>
 * @author: liuchen11
 */
public class IKTokenizerFactory extends TokenizerFactory {

    private boolean useSmart;

    public IKTokenizerFactory(boolean useSmart) {
        super(new HashMap<>());
        this.useSmart = useSmart;
    }

    public IKTokenizerFactory setSmart(boolean smart) {
        this.useSmart = smart;
        return this;
    }

    @Override
    public Tokenizer create(AttributeFactory factory) {
        return new IKTokenizer(this.useSmart);
    }
}
