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
package com.shaie.annots.filter;

import static org.fest.assertions.Assertions.*;

import static com.shaie.annots.filter.PreAnnotatedTokenFilter.*;

import java.io.IOException;
import java.io.StringReader;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.EmptyTokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.util.BytesRef;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableList;

/** Unit tests for {@link PreAnnotatedTokenFilter}. */
public class PreAnnotatedTokenFilterTest {

    private static final String ONE = "one";
    private static final String TWO = "two";
    private static final String THREE = "three";
    private static final String FOUR = "four";
    private static final String ONE_TWO = ONE + " " + TWO;
    private static final String ONE_TWO_THREE = ONE_TWO + " " + THREE;
    private static final String ONE_TWO_THREE_FOUR = ONE_TWO_THREE + " " + FOUR;

    @Rule
    public final ExpectedException expected = ExpectedException.none();

    @SuppressWarnings({ "unused", "resource" })
    @Test
    public void fails_if_no_annotation_markers() {
        expected.expect(IllegalArgumentException.class);
        new PreAnnotatedTokenFilter(new EmptyTokenStream());
    }

    @Test
    public void returns_false_when_no_more_tokens() throws IOException {
        try (TokenFilter f = new PreAnnotatedTokenFilter(new EmptyTokenStream(), 1, 2)) {
            f.reset();
            assertThat(f.incrementToken()).isFalse();
        }
    }

    @Test
    public void returns_annotated_token() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter f = new PreAnnotatedTokenFilter(tok, 0, 1)) {
            tok.setReader(new StringReader(ONE));
            assertTokenInfos(f, new TokenInfo(ANY_ANNOTATION_TERM, 0, 1), new TokenInfo(ONE, 0));
        }
    }

    @Test
    public void returns_all_annotated_tokens() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter f = new PreAnnotatedTokenFilter(tok, 0, 1, 2, 1)) {
            tok.setReader(new StringReader(ONE_TWO_THREE));
            assertTokenInfos(f, new TokenInfo(ANY_ANNOTATION_TERM, 0, 1), new TokenInfo(ONE, 0),
                    new TokenInfo(ANY_ANNOTATION_TERM, 2, 1), new TokenInfo(THREE, 2));
        }
    }

    @Test
    public void returns_tokens_when_only_annotated_tokens() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter f = new PreAnnotatedTokenFilter(tok, 0, 1, 1, 1)) {
            tok.setReader(new StringReader(ONE_TWO));
            assertTokenInfos(f, new TokenInfo(ANY_ANNOTATION_TERM, 0, 1), new TokenInfo(ONE, 0),
                    new TokenInfo(ANY_ANNOTATION_TERM, 1, 1), new TokenInfo(TWO, 1));
        }
    }

    @Test
    public void returns_tokens_when_underlying_stream_skips_over_tokens() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter stop = new StopFilter(tok, new CharArraySet(ImmutableList.of(ONE), false));
                TokenFilter f = new PreAnnotatedTokenFilter(stop, 1, 1)) {
            tok.setReader(new StringReader(ONE_TWO));
            assertTokenInfos(f, new TokenInfo(ANY_ANNOTATION_TERM, 1, 1), new TokenInfo(TWO, 1));
        }
    }

    @Test
    public void returns_token_when_underlying_stream_skips_multiple_tokens() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter stop = new StopFilter(tok, new CharArraySet(ImmutableList.of(ONE, THREE), false));
                TokenFilter f = new PreAnnotatedTokenFilter(stop, 1, 1, 3, 1)) {
            tok.setReader(new StringReader(ONE_TWO_THREE_FOUR));
            assertTokenInfos(f, new TokenInfo(ANY_ANNOTATION_TERM, 1, 1), new TokenInfo(TWO, 1),
                    new TokenInfo(ANY_ANNOTATION_TERM, 3, 1), new TokenInfo(FOUR, 3));
        }
    }

    @Test
    public void returns_tokens_when_annotation_markers_overlap() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter f = new PreAnnotatedTokenFilter(tok, 0, 1, 1, 1, 0, 2)) {
            tok.setReader(new StringReader(ONE_TWO));
            assertTokenInfos(f, new TokenInfo(ANY_ANNOTATION_TERM, 0, 2), new TokenInfo(ONE, 0), new TokenInfo(TWO, 1));
        }
    }

    @Test
    public void returns_tokens_when_annotation_markers_overlap_more_than_one_token() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter f = new PreAnnotatedTokenFilter(tok, 0, 1, 2, 1, 0, 3)) {
            tok.setReader(new StringReader(ONE_TWO_THREE));
            assertTokenInfos(f, new TokenInfo(ANY_ANNOTATION_TERM, 0, 3), new TokenInfo(ONE, 0), new TokenInfo(TWO, 1),
                    new TokenInfo(THREE, 2));
        }
    }

    @Test
    public void returns_tokens_when_annotated_tokens_are_filtered() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter stop = new StopFilter(tok, new CharArraySet(ImmutableList.of(TWO), false));
                TokenFilter f = new PreAnnotatedTokenFilter(stop, 0, 1, 1, 1, 0, 3)) {
            tok.setReader(new StringReader(ONE_TWO_THREE));
            assertTokenInfos(f, new TokenInfo(ANY_ANNOTATION_TERM, 0, 3), new TokenInfo(ONE, 0),
                    new TokenInfo(THREE, 2));
        }
    }

    @Test
    public void returns_tokens_when_adjacent_annotation_markers() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                TokenFilter f = new PreAnnotatedTokenFilter(tok, 0, 1, 1, 1, 0, 2, 2, 1, 3, 1, 2, 2)) {
            tok.setReader(new StringReader(ONE_TWO_THREE_FOUR));
            assertTokenInfos(f, new TokenInfo(ANY_ANNOTATION_TERM, 0, 2), new TokenInfo(ONE, 0), new TokenInfo(TWO, 1),
                    new TokenInfo(ANY_ANNOTATION_TERM, 2, 2), new TokenInfo(THREE, 2), new TokenInfo(FOUR, 3));
        }
    }

    private static void assertTokenInfos(TokenStream ts, TokenInfo... infos) throws IOException {
        ts.reset();
        final CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
        final PositionIncrementAttribute posIncrAtt = ts.addAttribute(PositionIncrementAttribute.class);
        final PayloadAttribute payloadAtt = ts.addAttribute(PayloadAttribute.class);
        final ByteArrayDataInput in = new ByteArrayDataInput();
        int pos = -1;
        for (final TokenInfo info : infos) {
            assertThat(ts.incrementToken()).isTrue();
            pos += posIncrAtt.getPositionIncrement();
            int len = -1;
            final BytesRef payload = payloadAtt.getPayload();
            if (info.len != -1) {
                assertThat(payload).isNotNull();
                in.reset(payload.bytes);
                len = in.readVInt();
            } else {
                assertThat(payload).isNull();
            }
            assertThat(new TokenInfo(term.toString(), pos, len)).isEqualTo(info);
        }
        assertThat(ts.incrementToken()).isFalse();
    }

    private static class TokenInfo {
        public final String term;
        public final int pos;
        public final int len;

        public TokenInfo(String term, int pos) {
            this(term, pos, -1);
        }

        public TokenInfo(String term, int pos, int len) {
            this.term = term;
            this.pos = pos;
            this.len = len;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(term)
                    .append(pos)
                    .append(len)
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
                    .append(len, other.len)
                    .isEquals();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("term", term)
                    .append("pos", pos)
                    .append("len", len)
                    .toString();
        }
    }

}