package com.shaie.annots;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.StringReader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.sinks.TeeSinkTokenFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.util.BytesRef;

/** Demonstrates {@link AnnotatingTokenFilter}. */
public class AnnotatingTokenStreamExample {

    public static final String COLOR_ANNOT_TERM = "color";

    public static void main(String[] args) throws Exception {
        String text = "quick brown fox ate the blue red chicken";
        Tokenizer tokenizer = new WhitespaceTokenizer();
        tokenizer.setReader(new StringReader(text));
        TeeSinkTokenFilter teeSink = new TeeSinkTokenFilter(tokenizer);
        TokenStream colors = new AnnotatingTokenFilter(teeSink.newSinkTokenStream(new ColorsSinkFilter()),
                COLOR_ANNOT_TERM);

        System.out.println("Text tokens:\n");

        // consume all the tokens from the original stream. this also populates the
        // Sink (colors) with its color-matching tokens
        teeSink.reset();
        CharTermAttribute termAtt = teeSink.getAttribute(CharTermAttribute.class);
        PositionIncrementAttribute termPosAtt = teeSink.getAttribute(PositionIncrementAttribute.class);
        int termsPos = -1;
        while (teeSink.incrementToken()) {
            termsPos += termPosAtt.getPositionIncrement();
            System.out.println("term=" + termAtt + ", pos=" + termsPos);
        }
        teeSink.end();
        tokenizer.end();

        System.out.println("\nAnnotation tokens:\n");

        // now consume the color annotation tokens from the colors stream
        CharTermAttribute colorAtt = colors.getAttribute(CharTermAttribute.class);
        PayloadAttribute payloadAtt = colors.getAttribute(PayloadAttribute.class);
        ByteArrayDataInput in = new ByteArrayDataInput();
        colors.reset();
        while (colors.incrementToken()) {
            BytesRef bytes = payloadAtt.getPayload();
            in.reset(bytes.bytes, bytes.offset, bytes.length);
            System.out.println("term=" + colorAtt + ", start=" + in.readVInt() + ", length=" + in.readVInt());
        }
        colors.end();
        colors.close();

        teeSink.close();
        tokenizer.close();
    }

}
