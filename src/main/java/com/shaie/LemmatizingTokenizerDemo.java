package com.shaie;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeImpl;

import com.google.common.base.Strings;

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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class LemmatizingTokenizerDemo {

    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception {
        final String text = "cars";
        System.out.println("Stem-only analyzer");
        printTokens(new StemOnlyAnalyzer().tokenStream("", new StringReader(text)));
        System.out.println("--------------------");
        System.out.println("Stem-and-original analyzer");
        printTokens(new StemAndOrigAnalyzer().tokenStream("", new StringReader(text)));
    }

    private static void printTokens(TokenStream tokenStream) throws IOException {
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            System.out.println(tokenStream);
        }
    }

    /** Analyzer that returns both the stem and the original token. */
    public static final class StemAndOrigAnalyzer extends Analyzer {

        @SuppressWarnings("resource")
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            final Tokenizer tokenizer = new LemmatizingTokenizer();
            TokenStream stream = new LowerCaseFilter(tokenizer);
            // stream = new KeywordRepeatFilter(stream);
            stream = new LemmaTokenFilter(stream, true);
            return new TokenStreamComponents(tokenizer, stream);
        }

    }

    /** Analyzer that returns only the stem of a token. */
    public static final class StemOnlyAnalyzer extends Analyzer {

        @SuppressWarnings("resource")
        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            final Tokenizer tokenizer = new LemmatizingTokenizer();
            TokenStream stream = new LowerCaseFilter(tokenizer);
            stream = new LemmaTokenFilter(stream, false);
            return new TokenStreamComponents(tokenizer, stream);
        }

    }

    public static final class LemmatizingTokenizer extends Tokenizer {

        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
        private final DocRefAttribute docRefAtt = addAttribute(DocRefAttribute.class);
        private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
        private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

        private final String[] tokens = { "cars", "not_stemmed" };
        private final String[] lemmas = { "car", null };
        private final int[] startOffsets = { 0, 4 };

        private State state;
        private int idx = 0;

        @Override
        public boolean incrementToken() throws IOException {
            if (state != null) {
                restoreState(state);
                posIncrAtt.setPositionIncrement(0);
                termAtt.setEmpty().append(lemmas[idx]);
            }

            if (idx >= tokens.length) { // no more tokens
                return false;
            }

            posIncrAtt.setPositionIncrement(1);
            termAtt.setEmpty().append(tokens[idx]);
            offsetAtt.setOffset(startOffsets[idx], startOffsets[idx] + tokens[idx].length());
            // set additional attributes, such as offsets..
            // setAttribute()
            // setAttribute()
            state = captureState(); // capture the state of all attributes

            docRefAtt.setToken(tokens[idx]);
            if (idx == 0) {
                docRefAtt.setLemma("car"); // that would be the lemma
            } else {
                docRefAtt.setLemma(null); // some tokens may not have a lemma!
            }

            ++idx;
            return true;
        }

        @Override
        public void reset() throws IOException {
            super.reset();
            idx = 0;
            state = null;
        }
    }

    static final class LemmaTokenFilter extends TokenFilter {

        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
        private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
        private final DocRefAttribute docRefAtt = addAttribute(DocRefAttribute.class);

        private final boolean preserveOriginalToken;

        private boolean returnedLemma = false;
        private boolean returnedOriginalToken = false;
        private boolean handledLemma = false;

        public LemmaTokenFilter(TokenStream input, boolean preserveOriginalToken) {
            super(input);
            this.preserveOriginalToken = preserveOriginalToken;
        }

        @Override
        public boolean incrementToken() throws IOException {
            // If we need to return the original token, do so before consuming the next token from the stream.
            if (handledLemma && !returnedOriginalToken) {
                termAtt.setEmpty().append(docRefAtt.token());
                if (returnedLemma) {
                    posIncrAtt.setPositionIncrement(0); // Original token is returned at the same position as the lemma.
                }
                returnedOriginalToken = true;
                return true;
            }

            // Consume the next token from the stream.
            if (!input.incrementToken()) {
                return false;
            }

            handledLemma = true;

            // If there is a lemma, return it first.
            final String lemma = docRefAtt.lemma();
            if (!Strings.isNullOrEmpty(lemma)) {
                termAtt.setEmpty().append(lemma);
                returnedLemma = true;
                // Mark to return the original token, if needed.
                returnedOriginalToken = !preserveOriginalToken;
                return true;
            }

            // There is no lemma, so proceed to return the original token.
            returnedLemma = false;
            returnedOriginalToken = false;
            return incrementToken();
        }

        @Override
        public void reset() throws IOException {
            super.reset();
            handledLemma = false;
            returnedLemma = false;
            returnedOriginalToken = false;
        }
    }

    public static interface DocRefAttribute extends Attribute {
        public String token();

        public void setToken(String token);

        public String lemma();

        public void setLemma(String lemma);

        public void clear();
    }

    public static final class DocRefAttributeImpl extends AttributeImpl implements DocRefAttribute {

        private String token;
        private String lemma;

        @Override
        public void clear() {
            token = null;
            lemma = null;
        }

        @Override
        public void copyTo(AttributeImpl target) {
            final DocRefAttribute other = (DocRefAttribute) target;
            other.setToken(token);
            other.setLemma(lemma);
        }

        @Override
        public String toString() {
            return "DocRefAttribute token=" + token + ", lemma=" + lemma;
        }

        @Override
        public String token() {
            return token;
        }

        @Override
        public void setToken(String token) {
            this.token = token;
        }

        @Override
        public String lemma() {
            return lemma;
        }

        @Override
        public void setLemma(String lemma) {
            this.lemma = lemma;
        }
    }

}