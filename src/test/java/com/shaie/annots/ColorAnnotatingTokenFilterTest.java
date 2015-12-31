package com.shaie.annots;

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

import static org.fest.assertions.Assertions.*;

import java.io.IOException;
import java.io.StringReader;

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
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.util.BytesRef;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ColorAnnotatingTokenFilterTest {

    private static final String ANY_TERM = "ANY_TERM";
    private static final String TEXT_WITHOUT_COLORS = "quick fox and a slow dog";
    private static final String TEXT_WITH_COLOR = "quick brown fox";
    private static final String TEXT_WITH_COLORS = "quick brown and red fox";
    private static final String TEXT_WITH_ONLY_COLORS = "brown blue black";
    private static final String TEXT_WITH_COLOR_AND_STOPWORDS = "quick and brown fox";

    @Test
    public void returns_false_when_no_more_tokens() throws IOException {
        try (ColorAnnotatingTokenFilter f = new ColorAnnotatingTokenFilter(new EmptyTokenStream(), ANY_TERM)) {
            f.reset();
            assertThat(f.incrementToken()).isFalse();
        }
    }

    @Test
    public void does_not_return_any_token_if_no_color_tokens() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                ColorAnnotatingTokenFilter f = new ColorAnnotatingTokenFilter(tok, ANY_TERM)) {
            tok.setReader(new StringReader(TEXT_WITHOUT_COLORS));
            assertAnnotationInfos(f, new AnnotationInfo[0]);
        }
    }

    @Test
    public void returns_proper_token_annotation_info_for_color_token() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                ColorAnnotatingTokenFilter f = new ColorAnnotatingTokenFilter(tok, ANY_TERM)) {
            tok.setReader(new StringReader(TEXT_WITH_COLOR));
            assertAnnotationInfos(f, new AnnotationInfo(1, 1));
        }
    }

    @Test
    public void returns_proper_token_annotation_info_for_color_tokens() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                ColorAnnotatingTokenFilter f = new ColorAnnotatingTokenFilter(tok, ANY_TERM)) {
            tok.setReader(new StringReader(TEXT_WITH_COLORS));
            assertAnnotationInfos(f, new AnnotationInfo(1, 1), new AnnotationInfo(3, 1));
        }
    }

    @Test
    public void returns_proper_token_annotation_info_when_only_color_tokens() throws IOException {
        try (Tokenizer tok = new WhitespaceTokenizer();
                ColorAnnotatingTokenFilter f = new ColorAnnotatingTokenFilter(tok, ANY_TERM)) {
            tok.setReader(new StringReader(TEXT_WITH_ONLY_COLORS));
            assertAnnotationInfos(f, new AnnotationInfo(0, 1), new AnnotationInfo(1, 1), new AnnotationInfo(2, 1));
        }
    }

    @Test
    public void returns_proper_token_annotation_info_when_underlying_stream_skips_over_tokens() throws IOException {
        try (final Tokenizer tok = new WhitespaceTokenizer();
                final TokenFilter stop = new StopFilter(tok, new CharArraySet(ImmutableList.of("quick"), false));
                ColorAnnotatingTokenFilter f = new ColorAnnotatingTokenFilter(stop, ANY_TERM)) {
            tok.setReader(new StringReader(TEXT_WITH_COLOR));
            assertAnnotationInfos(f, new AnnotationInfo(1, 1));
        }
    }

    @Test
    public void returns_proper_token_annotation_info_when_underlying_stream_skips_multiple_tokens() throws IOException {
        try (final Tokenizer tok = new WhitespaceTokenizer();
                final TokenFilter stop = new StopFilter(tok, new CharArraySet(ImmutableList.of("quick", "and"), false));
                ColorAnnotatingTokenFilter f = new ColorAnnotatingTokenFilter(stop, ANY_TERM)) {
            tok.setReader(new StringReader(TEXT_WITH_COLOR_AND_STOPWORDS));
            assertAnnotationInfos(f, new AnnotationInfo(2, 1));
        }
    }

    private void assertAnnotationInfos(TokenStream ts, AnnotationInfo... infos) throws IOException {
        ts.reset();
        final CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
        final PayloadAttribute payload = ts.addAttribute(PayloadAttribute.class);
        for (final AnnotationInfo info : infos) {
            assertThat(ts.incrementToken()).isTrue();
            assertThat(term.toString()).isEqualTo(ANY_TERM);
            assertThat(AnnotationInfo.fromPayload(payload.getPayload())).isEqualTo(info);
        }
        assertThat(ts.incrementToken()).isFalse();
    }

    private static class AnnotationInfo {
        public final int pos;
        public final int len;

        public static AnnotationInfo fromPayload(BytesRef bytes) {
            final ByteArrayDataInput in = new ByteArrayDataInput(bytes.bytes, 0, bytes.length);
            return new AnnotationInfo(in.readVInt(), in.readVInt());
        }

        public AnnotationInfo(int pos, int len) {
            this.pos = pos;
            this.len = len;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
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

            final AnnotationInfo other = (AnnotationInfo) obj;
            return new EqualsBuilder()
                    .append(pos, other.pos)
                    .append(len, other.len)
                    .isEquals();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("pos", pos)
                    .append("len", len)
                    .toString();
        }
    }

}