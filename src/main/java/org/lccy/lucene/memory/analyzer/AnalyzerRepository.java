package org.lccy.lucene.memory.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.apache.lucene.analysis.pattern.PatternTokenizerFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.elasticsearch.analysis.PinyinConfig;
import org.lccy.lucene.memory.exception.LuceneException;
import org.lccy.lucene.memory.util.StringUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MemoryIndex的分词器仓库，提供了一些默认的分词器
 *
 * @Date: 2023/11/23 19:14 <br>
 * @author: liuchen11
 */
public final class AnalyzerRepository {

    private AnalyzerRepository() {
    }

    private static Map<String, Analyzer> analyzerMap = new ConcurrentHashMap<>();

    static {
        // CharFilter
        Map<String, String> punctCharFilterSetting = new HashMap<>();
        punctCharFilterSetting.put("pattern", "[\\p{Punct}\\pP]");
        punctCharFilterSetting.put("replacement", "");
        Map<String, String> douhaoPunctCharFilterSetting = new HashMap<>();
        douhaoPunctCharFilterSetting.put("pattern", "[\\p{Punct}\\pP&&[^,、 ]]");
        douhaoPunctCharFilterSetting.put("replacement", "");

        CharFilterFactory punctCharFilterFactory = new PatternReplaceCharFilterFactory(punctCharFilterSetting);
        CharFilterFactory douhaoPunctCharFilterFactory = new PatternReplaceCharFilterFactory(douhaoPunctCharFilterSetting);

        CharFilterFactory[] charFilters = new CharFilterFactory[]{punctCharFilterFactory};
        CharFilterFactory[] douhaoCharFilters = new CharFilterFactory[]{douhaoPunctCharFilterFactory};

        // Tokenizer
        Map<String, String> douhaoPunctSetting = new HashMap<>();
        douhaoPunctSetting.put(PatternTokenizerFactory.PATTERN, "[,、 ]");
        douhaoPunctSetting.put(PatternTokenizerFactory.GROUP, "-1");

        TokenizerFactory douhaoPunctTokenizerFactory = new PatternTokenizerFactory(douhaoPunctSetting);
        TokenizerFactory ikMaxWordFactory = new IKTokenizerFactory(false);
        TokenizerFactory ikSmartWordFactory = new IKTokenizerFactory(true);

        Map<String, String> douhaoSetting = new HashMap<>();
        douhaoSetting.put(PatternTokenizerFactory.PATTERN, ",");
        douhaoSetting.put(PatternTokenizerFactory.GROUP, "-1");

        TokenizerFactory douhaoTokenizerFactory = new PatternTokenizerFactory(douhaoSetting);

        List<String> tokenChars = Arrays.asList("letter", "digit");
        EdgeNGramTokenizerFactory edgeNGramTokenizerFactory = new EdgeNGramTokenizerFactory(1, 50, tokenChars);

        WhitespaceTokenizerFactory whitespaceTokenizerFactory = new WhitespaceTokenizerFactory(new HashMap<>());


        // TokenFilter
        TokenFilterFactory lowerCaseFilterFactory = new LowerCaseFilterFactory(new HashMap<>());

        PinyinConfig pinyinSimpleFilterConfig = new PinyinConfig();
        pinyinSimpleFilterConfig.keepFirstLetter = true;
        pinyinSimpleFilterConfig.keepFullPinyin = false;
        pinyinSimpleFilterConfig.noneChinesePinyinTokenize = false;
        pinyinSimpleFilterConfig.keepOriginal = false;
        pinyinSimpleFilterConfig.LimitFirstLetterLength = 50;
        pinyinSimpleFilterConfig.keepNoneChinese = true;
        pinyinSimpleFilterConfig.keepNoneChineseTogether = true;
        TokenFilterFactory pinyinSimpleFilterFactory = new PinyinTokenFilterFactory(pinyinSimpleFilterConfig);

        PinyinConfig pinyinFullFilterConfig = new PinyinConfig();
        pinyinFullFilterConfig.keepFirstLetter = false;
        pinyinFullFilterConfig.LimitFirstLetterLength = 50;
        pinyinFullFilterConfig.keepFullPinyin = false;
        pinyinFullFilterConfig.keepJoinedFullPinyin = true;
        pinyinFullFilterConfig.noneChinesePinyinTokenize = false;
        pinyinFullFilterConfig.keepOriginal = false;
        TokenFilterFactory pinyinFullFilterFactory = new PinyinTokenFilterFactory(pinyinFullFilterConfig);

        PinyinConfig pinyinOnlyFullFilterConfig = new PinyinConfig();
        pinyinOnlyFullFilterConfig.keepFirstLetter = false;
        pinyinOnlyFullFilterConfig.keepFullPinyin = true;
        pinyinOnlyFullFilterConfig.keepJoinedFullPinyin = false;
        pinyinOnlyFullFilterConfig.keepNoneChineseInFirstLetter = false;
        pinyinOnlyFullFilterConfig.keepNoneChinese = false;
        pinyinOnlyFullFilterConfig.keepNoneChineseTogether = false;
        pinyinOnlyFullFilterConfig.keepOriginal = false;
        TokenFilterFactory pinyinOnlyFullFilterFactory = new PinyinTokenFilterFactory(pinyinOnlyFullFilterConfig);

        PinyinConfig pinyinDouhaoFullFilterConfig = new PinyinConfig();
        pinyinDouhaoFullFilterConfig.keepFirstLetter = false;
        pinyinDouhaoFullFilterConfig.keepFullPinyin = false;
        pinyinDouhaoFullFilterConfig.keepJoinedFullPinyin = true;
        pinyinDouhaoFullFilterConfig.keepNoneChinese = false;
        pinyinDouhaoFullFilterConfig.keepNoneChineseTogether = false;
        pinyinDouhaoFullFilterConfig.keepNoneChineseInJoinedFullPinyin = true;
        pinyinDouhaoFullFilterConfig.keepOriginal = false;
        TokenFilterFactory pinyinDouhaoFullFilterFactory = new PinyinTokenFilterFactory(pinyinDouhaoFullFilterConfig);

        PinyinConfig pinyinDouhaoSimpFilterConfig = new PinyinConfig();
        pinyinDouhaoSimpFilterConfig.keepFirstLetter = true;
        pinyinDouhaoSimpFilterConfig.keepNoneChineseInFirstLetter = true;
        pinyinDouhaoSimpFilterConfig.keepFullPinyin = false;
        pinyinDouhaoSimpFilterConfig.keepJoinedFullPinyin = false;
        pinyinDouhaoSimpFilterConfig.keepNoneChinese = false;
        pinyinDouhaoSimpFilterConfig.keepNoneChineseTogether = false;
        pinyinDouhaoSimpFilterConfig.keepOriginal = false;
        TokenFilterFactory pinyinDouhaoSimpFilterFactory = new PinyinTokenFilterFactory(pinyinDouhaoSimpFilterConfig);

        PinyinConfig pinyinNgramSimpleFilterConfig = new PinyinConfig();
        pinyinNgramSimpleFilterConfig.keepFirstLetter = true;
        pinyinNgramSimpleFilterConfig.keepFullPinyin = false;
        pinyinNgramSimpleFilterConfig.noneChinesePinyinTokenize = false;
        pinyinNgramSimpleFilterConfig.keepOriginal = false;
        pinyinNgramSimpleFilterConfig.LimitFirstLetterLength = 50;
        pinyinNgramSimpleFilterConfig.keepNoneChinese = true;
        pinyinNgramSimpleFilterConfig.keepNoneChineseTogether = true;
        TokenFilterFactory pinyinNgramSimpleFilterFactory = new PinyinTokenFilterFactory(pinyinNgramSimpleFilterConfig);

        PinyinConfig pinyinNgramFullFilterConfig = new PinyinConfig();
        pinyinNgramFullFilterConfig.keepFirstLetter = false;
        pinyinNgramFullFilterConfig.LimitFirstLetterLength = 50;
        pinyinNgramFullFilterConfig.keepFullPinyin = false;
        pinyinNgramFullFilterConfig.keepJoinedFullPinyin = true;
        pinyinNgramFullFilterConfig.noneChinesePinyinTokenize = false;
        pinyinNgramFullFilterConfig.keepNoneChineseInJoinedFullPinyin = true;
        pinyinNgramFullFilterConfig.keepOriginal = false;
        TokenFilterFactory pinyinNgramFullFilterFactory = new PinyinTokenFilterFactory(pinyinNgramFullFilterConfig);

        CustomAnalyzer ikMaxWord = new CustomAnalyzer(charFilters, ikMaxWordFactory, null);
        CustomAnalyzer ikSmartWord = new CustomAnalyzer(charFilters, ikSmartWordFactory, null);
        CustomAnalyzer ikSmartSimplePinyin = new CustomAnalyzer(charFilters, ikSmartWordFactory, new TokenFilterFactory[]{pinyinSimpleFilterFactory, lowerCaseFilterFactory});
        CustomAnalyzer ikSmartFullPinyin = new CustomAnalyzer(charFilters, ikSmartWordFactory, new TokenFilterFactory[]{pinyinFullFilterFactory, lowerCaseFilterFactory});
        CustomAnalyzer standardFullWord = new CustomAnalyzer(charFilters, new StandardTokenizerFactory(new HashMap<>()), new TokenFilterFactory[]{pinyinOnlyFullFilterFactory, lowerCaseFilterFactory});
        CustomAnalyzer douhao = new CustomAnalyzer(null, douhaoTokenizerFactory, null);
        CustomAnalyzer douhaoPunct = new CustomAnalyzer(douhaoCharFilters, douhaoPunctTokenizerFactory, null);
        CustomAnalyzer douhaoFullPy = new CustomAnalyzer(douhaoCharFilters, douhaoPunctTokenizerFactory, new TokenFilterFactory[]{pinyinDouhaoFullFilterFactory, lowerCaseFilterFactory});
        CustomAnalyzer douhaoSimplePy = new CustomAnalyzer(douhaoCharFilters, douhaoPunctTokenizerFactory, new TokenFilterFactory[]{pinyinDouhaoSimpFilterFactory, lowerCaseFilterFactory});
        CustomAnalyzer edgeNgramWord = new CustomAnalyzer(charFilters, edgeNGramTokenizerFactory, new TokenFilterFactory[]{lowerCaseFilterFactory});
        CustomAnalyzer edgeNgramSimplePinyin = new CustomAnalyzer(charFilters, edgeNGramTokenizerFactory, new TokenFilterFactory[]{pinyinNgramSimpleFilterFactory, lowerCaseFilterFactory});
        CustomAnalyzer edgeNgramFullPinyin = new CustomAnalyzer(charFilters, edgeNGramTokenizerFactory, new TokenFilterFactory[]{pinyinNgramFullFilterFactory, lowerCaseFilterFactory});
        CustomAnalyzer whitespace = new CustomAnalyzer(charFilters, whitespaceTokenizerFactory, new TokenFilterFactory[]{lowerCaseFilterFactory});
        CustomAnalyzer whitespaceSimplePinyin = new CustomAnalyzer(charFilters, whitespaceTokenizerFactory, new TokenFilterFactory[]{pinyinNgramSimpleFilterFactory, lowerCaseFilterFactory});
        CustomAnalyzer whitespaceFullPinyin = new CustomAnalyzer(charFilters, whitespaceTokenizerFactory, new TokenFilterFactory[]{pinyinNgramFullFilterFactory, lowerCaseFilterFactory});


        analyzerMap.put("ik_max_word", ikMaxWord);
        analyzerMap.put("ik_smart_word", ikSmartWord);
        analyzerMap.put("ik_smart_simple_word", ikSmartSimplePinyin);
        analyzerMap.put("ik_smart_full_word", ikSmartFullPinyin);
        analyzerMap.put("standard_full_word", standardFullWord);
        analyzerMap.put("douhao", douhao);
        analyzerMap.put("douhao_punct", douhaoPunct);
        analyzerMap.put("douhao_full_word", douhaoFullPy);
        analyzerMap.put("douhao_simple_word", douhaoSimplePy);
        analyzerMap.put("edge_ngram_word", edgeNgramWord);
        analyzerMap.put("edge_ngram_simple_word", edgeNgramSimplePinyin);
        analyzerMap.put("edge_ngram_full_word", edgeNgramFullPinyin);
        analyzerMap.put("whitespace", whitespace);
        analyzerMap.put("whitespace_simple_word", whitespaceSimplePinyin);
        analyzerMap.put("whitespace_full_word", whitespaceFullPinyin);
    }

    /**
     * 获取分词器
     *
     * @param analyzer
     * @return
     * @throws LuceneException
     */
    public static Analyzer getAnalyzer(String analyzer) throws LuceneException {
        if (StringUtil.isEmpty(analyzer)) {
            return new KeywordAnalyzer();
        }
        Analyzer result = analyzerMap.get(analyzer);
        if (result == null) {
            throw new LuceneException("Analyzer:" + analyzer + " not exists");
        }
        return result;
    }

    /**
     * 注册自定义分词器，分词器必须是线程安全的，因为是单例维护
     *
     * @param analyzerName
     * @param analyzer
     */
    public static void register(String analyzerName, Analyzer analyzer) {
        if (StringUtil.isEmpty(analyzerName) || analyzer == null) {
            throw new IllegalArgumentException("Analyzer register error, param is empty.");
        }
        analyzerMap.put(analyzerName, analyzer);
    }

}
