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
package com.shaie.annots;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.EmptyTokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.shaie.annots.annotator.Annotator;

/** Unit tests for {@link AnnotatorTokenFilter}. */
public class AnnotatorTokenFilterTest {

    private static final String ONE = "one";
    private static final String TWO = "two";
    private static final String THREE = "three";
    private static final String FOUR = "four";
    private static final String ONE_TWO = ONE + " " + TWO;
    private static final String ONE_TWO_THREE = ONE_TWO + " " + THREE;
    private static final String ONE_TWO_THREE_FOUR = ONE_TWO_THREE + " " + FOUR;

    private final Annotator annotator = mock(Annotator.class);

    @Before
    public void setUp() {
        stubAnnotator();
    }

    @Test
    public void returns_false_when_no_more_tokens() throws IOException {
        try (TokenFilter f = new AnnotatorTokenFilter(new EmptyTokenStream(), annotator)) {
            f.reset();
            assertThat(f.incrementToken()).isFalse();
        }
    }

    @Test
    public void does_not_return_any_token_if_no_accepted_tokens() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter f = new AnnotatorTokenFilter(tok, annotator)) {
            tok.setReader(new StringReader(ONE));
            assertTokenInfos(f);
        }
    }

    @Test
    public void returns_accepted_token() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter f = new AnnotatorTokenFilter(tok, annotator)) {
            stubAnnotator(ONE);
            tok.setReader(new StringReader(ONE));
            assertTokenInfos(f, new TokenInfo(ONE, 0));
        }
    }

    @Test
    public void returns_all_accepted_tokens() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter f = new AnnotatorTokenFilter(tok, annotator)) {
            stubAnnotator(ONE, THREE);
            tok.setReader(new StringReader(ONE_TWO_THREE));
            assertTokenInfos(f, new TokenInfo(ONE, 0), new TokenInfo(THREE, 2));
        }
    }

    @Test
    public void returns_tokens_when_only_accepted_tokens() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter f = new AnnotatorTokenFilter(tok, annotator)) {
            stubAnnotator(ONE, TWO);
            tok.setReader(new StringReader(ONE_TWO));
            assertTokenInfos(f, new TokenInfo(ONE, 0), new TokenInfo(TWO, 1));
        }
    }

    @Test
    public void returns_tokens_when_underlying_stream_skips_over_tokens() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter stop = new StopFilter(tok, new CharArraySet(ImmutableList.of(ONE), false));
                TokenFilter f = new AnnotatorTokenFilter(stop, annotator)) {
            stubAnnotator(TWO);
            tok.setReader(new StringReader(ONE_TWO));
            assertTokenInfos(f, new TokenInfo(TWO, 1));
        }
    }

    @Test
    public void returns_token_when_underlying_stream_skips_multiple_tokens() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter stop = new StopFilter(tok, new CharArraySet(ImmutableList.of(ONE, THREE), false));
                TokenFilter f = new AnnotatorTokenFilter(stop, annotator)) {
            stubAnnotator(TWO, FOUR);
            tok.setReader(new StringReader(ONE_TWO_THREE_FOUR));
            assertTokenInfos(f, new TokenInfo(TWO, 1), new TokenInfo(FOUR, 3));
        }
    }

    private void stubAnnotator(String... termsToAccept) {
        final Answer<Boolean> answer = new AcceptingAnswer(Sets.newHashSet(termsToAccept));
        when(annotator.accept(anyString())).thenAnswer(answer);
        when(annotator.accept(any(char[].class), anyInt(), anyInt())).then(answer);
    }

    private void assertTokenInfos(TokenStream ts, TokenInfo... infos) throws IOException {
        ts.reset();
        final CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
        final PositionIncrementAttribute posIncrAtt = ts.addAttribute(PositionIncrementAttribute.class);
        int pos = -1;
        for (final TokenInfo info : infos) {
            assertThat(ts.incrementToken()).isTrue();
            pos += posIncrAtt.getPositionIncrement();
            assertThat(new TokenInfo(term.toString(), pos)).isEqualTo(info);
        }
        assertThat(ts.incrementToken()).isFalse();
    }

    private static class AcceptingAnswer implements Answer<Boolean> {

        private final Set<String> terms;

        public AcceptingAnswer(Set<String> terms) {
            this.terms = terms;
        }

        @Override
        public Boolean answer(InvocationOnMock invocation) throws Throwable {
            final Object[] args = invocation.getArguments();
            if (args[0] == null) {
                return false;
            }
            final String term;
            if (args.length == 1) { // accept(String) variant
                term = (String) args[0];
            } else { // accept(char[], int, int) variant
                term = new String((char[]) args[0], (int) args[1], (int) args[2]);
            }
            return terms.contains(term);
        }
    }

    private static class TokenInfo {
        public final String term;
        public final int pos;

        public TokenInfo(String term, int pos) {
            this.term = term;
            this.pos = pos;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(term)
                    .append(pos)
                    .toHashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            final TokenInfo other = (TokenInfo) obj;
            return new EqualsBuilder()
                    .append(term, other.term)
                    .append(pos, other.pos)
                    .isEquals();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("term", term)
                    .append("pos", pos)
                    .toString();
        }
    }

}