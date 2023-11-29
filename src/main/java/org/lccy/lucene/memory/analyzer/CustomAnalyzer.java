package org.lccy.lucene.memory.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;

import java.io.Reader;

/**
 * 自定义lucene的Analyzer组合器，支持CharFilter、Tokenizer、TokenFilter自由组合
 *
 * @Date: 2023/11/23 18:00 <br>
 * @author: liuchen11
 */
public class CustomAnalyzer extends Analyzer {

    private CharFilterFactory[] charFilterFactories;
    private TokenizerFactory tokenizerFactory;
    private TokenFilterFactory[] tokenFilterFactorys;

    public CustomAnalyzer(CharFilterFactory[] charFilterFactories, TokenizerFactory tokenizerFactory, TokenFilterFactory[] tokenFilterFactorys) {
        this.charFilterFactories = charFilterFactories;
        this.tokenizerFactory = tokenizerFactory;
        this.tokenFilterFactorys = tokenFilterFactorys;
    }

    @Override
    protected Reader initReader(String fieldName, Reader reader) {
        if (this.charFilterFactories != null && this.charFilterFactories.length > 0) {
            for (CharFilterFactory charFilter : this.charFilterFactories) {
                reader = charFilter.create(reader);
            }
        }
        return reader;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = this.tokenizerFactory.create();
        TokenStream tokenStream = tokenizer;
        if (this.tokenFilterFactorys != null && this.tokenFilterFactorys.length > 0) {
            for (TokenFilterFactory tokenFilter : this.tokenFilterFactorys) {
                tokenStream = tokenFilter.create(tokenStream);
            }
        }
        return new TokenStreamComponents(tokenizer, tokenStream);
    }

    @Override
    protected Reader initReaderForNormalization(String fieldName, Reader reader) {
        if (this.charFilterFactories != null && this.charFilterFactories.length > 0) {
            for (CharFilterFactory charFilter : this.charFilterFactories) {
                reader = charFilter.normalize(reader);
            }
        }
        return reader;
    }

    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        TokenStream result = in;
        if (this.tokenFilterFactorys != null && this.tokenFilterFactorys.length > 0) {
            for (TokenFilterFactory filter : this.tokenFilterFactorys) {
                result = filter.normalize(result);
            }
        }

        return result;
    }
}
