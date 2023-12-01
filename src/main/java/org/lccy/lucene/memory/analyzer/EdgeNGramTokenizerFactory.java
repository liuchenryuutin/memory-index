/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.lccy.lucene.memory.analyzer;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenizer;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class EdgeNGramTokenizerFactory extends TokenizerFactory {

    private static final Map<String, CharMatcher> MATCHERS;
    private final int minGram;
    private final int maxGram;
    private final CharMatcher matcher;

    static {
        Map<String, CharMatcher> matchers = new HashMap<>();
        matchers.put("letter", CharMatcher.Basic.LETTER);
        matchers.put("digit", CharMatcher.Basic.DIGIT);
        matchers.put("whitespace", CharMatcher.Basic.WHITESPACE);
        matchers.put("punctuation", CharMatcher.Basic.PUNCTUATION);
        matchers.put("symbol", CharMatcher.Basic.SYMBOL);
        // Populate with unicode categories from java.lang.Character
        for (Field field : Character.class.getFields()) {
            if (!field.getName().startsWith("DIRECTIONALITY")
                    && Modifier.isPublic(field.getModifiers())
                    && Modifier.isStatic(field.getModifiers())
                    && field.getType() == byte.class) {
                try {
                    matchers.put(field.getName().toLowerCase(Locale.ROOT), CharMatcher.ByUnicodeCategory.of(field.getByte(null)));
                } catch (Exception e) {
                    // ignore
                    continue;
                }
            }
        }
        MATCHERS = Collections.unmodifiableMap(matchers);
    }

    public EdgeNGramTokenizerFactory(int minGram, int maxGram, List<String> tokenChars) {
        super(new HashMap<>());
        this.minGram = minGram;
        this.maxGram = maxGram;
        this.matcher = parseTokenChars(tokenChars);
    }

    @Override
    public Tokenizer create(AttributeFactory factory) {
        if (matcher == null) {
            return new EdgeNGramTokenizer(minGram, maxGram);
        } else {
            return new EdgeNGramTokenizer(minGram, maxGram) {
                @Override
                protected boolean isTokenChar(int chr) {
                    return matcher.isTokenChar(chr);
                }
            };
        }
    }

    static CharMatcher parseTokenChars(List<String> characterClasses) {
        if (characterClasses == null || characterClasses.isEmpty()) {
            return null;
        }
        CharMatcher.Builder builder = new CharMatcher.Builder();
        for (String characterClass : characterClasses) {
            characterClass = characterClass.toLowerCase(Locale.ROOT).trim();
            CharMatcher matcher = MATCHERS.get(characterClass);
            if (matcher == null) {
                throw new IllegalArgumentException("Unknown token type: '" + characterClass + "', must be one of " + MATCHERS.keySet());
            }
            builder.or(matcher);
        }
        return builder.build();
    }
}
