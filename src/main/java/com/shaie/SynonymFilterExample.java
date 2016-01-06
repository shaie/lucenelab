/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required byOCP applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shaie;

import java.io.StringReader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.CharsRefBuilder;

public class SynonymFilterExample {

    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception {
        final Tokenizer tok = new WhitespaceTokenizer();
        tok.setReader(new StringReader("dark sea green sea green"));

        final SynonymMap.Builder builder = new SynonymMap.Builder(true);
        addSynonym("dark sea green", "color", builder);
        addSynonym("green", "color", builder);
        addSynonym("dark sea", "color", builder);
        addSynonym("sea green", "color", builder);
        final SynonymMap synMap = builder.build();
        final TokenStream ts = new SynonymFilter(tok, synMap, true);

        final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
        final PositionIncrementAttribute posIncrAtt = ts.addAttribute(PositionIncrementAttribute.class);
        final PositionLengthAttribute posLengthAtt = ts.addAttribute(PositionLengthAttribute.class);

        ts.reset();
        int pos = -1;
        while (ts.incrementToken()) {
            pos += posIncrAtt.getPositionIncrement();
            System.out.println("term=" + termAtt + ", pos=" + pos + ", posLen=" + posLengthAtt.getPositionLength());
        }
        ts.end();
        ts.close();
    }

    private static void addSynonym(String input, String output, SynonymMap.Builder builder) {
        final CharsRef inputWords = SynonymMap.Builder.join(input.split(" "), new CharsRefBuilder());
        final CharsRef outputWords = SynonymMap.Builder.join(output.split(" "), new CharsRefBuilder());
        builder.add(inputWords, outputWords, true);
    }

}