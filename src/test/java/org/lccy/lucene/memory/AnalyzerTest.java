package org.lccy.lucene.memory;

import com.alibaba.fastjson.JSON;
import org.lccy.lucene.memory.analyzer.AnalyzerRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 类名称： <br>
 * 类描述： <br>
 *
 * @Date: 2023/11/29 10:15 <br>
 * @author: liuchen11
 */
public class AnalyzerTest {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerTest.class);

    @Test
    public void Test_analyzer() throws IOException {
        String input = "-融合*/-/套餐享宽=-带电视";
        Analyzer ik_max_word = AnalyzerRepository.getAnalyzer("ik_max_word");
        Analyzer ik_smart = AnalyzerRepository.getAnalyzer("ik_smart");
        Analyzer ik_smart_simple_word = AnalyzerRepository.getAnalyzer("ik_smart_simple_word");
        Analyzer ik_smart_full_word = AnalyzerRepository.getAnalyzer("ik_smart_full_word");
        Analyzer kms_standard_full_word = AnalyzerRepository.getAnalyzer("kms_standard_full_word");
        Analyzer douhao = AnalyzerRepository.getAnalyzer("douhao");
        Analyzer douhao_punct = AnalyzerRepository.getAnalyzer("douhao_punct");
        Analyzer douhao_full_py = AnalyzerRepository.getAnalyzer("douhao_full_py");
        Analyzer douhao_simple_py = AnalyzerRepository.getAnalyzer("douhao_simple_py");

        Analyzer analyzer = ik_max_word;

        StringReader reader = new StringReader(input);
        TokenStream tokenStream = analyzer.tokenStream("test", reader);
        CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
        // 重置TokenStream
        tokenStream.reset();
        // 遍历TokenStream并输出结果
        List<WordTerm> terms = new ArrayList<>();
        while (tokenStream.incrementToken()) {
            // 添加term
            terms.add(new WordTerm(termAttribute.toString(), offsetAttribute.startOffset(), offsetAttribute.endOffset()));
        }
        logger.info("terms:{}", JSON.toJSONString(terms));

    }

    @Setter
    @Getter
    @AllArgsConstructor
    public static class WordTerm {
        private String term;
        private int start;
        private int end;
    }

}
