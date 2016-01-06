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

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.store.ByteArrayDataOutput;
import org.apache.lucene.util.BytesRef;

import com.google.common.collect.ImmutableList;

/**
 * A {@link TokenFilter} which annotates terms that are recognized as colors. The filter discards all terms that are
 * returned by the input stream, and returns the same term value for each original term that is identified as a color.
 * In addition it stores in the term's payload the position of the term in the original text and the annotation length
 * (usually 1), encoded as two {@link org.apache.lucene.store.DataOutput#writeVInt(int) VInt}s.
 */
public final class ColorAnnotatingTokenFilter extends TokenFilter {

    /** Recognized colors. This ideally will be provided in the constructor. */
    private static final CharArraySet COLORS = new CharArraySet(
            ImmutableList.of("black", "blue", "brown", "red", "green"), true);

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PayloadAttribute payloadAtt = addAttribute(PayloadAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final BytesRef payload = new BytesRef(10); // max 5 bytes per VInt
    private final ByteArrayDataOutput out = new ByteArrayDataOutput();
    private final String term;

    private int absTextPos = -1;

    public ColorAnnotatingTokenFilter(TokenStream input, String term) {
        super(input);
        this.term = term;
    }

    @Override
    public boolean incrementToken() throws IOException {
        while (input.incrementToken()) {
            absTextPos += posIncrAtt.getPositionIncrement(); // Update the absolute text position.
            if (COLORS.contains(termAtt.buffer(), 0, termAtt.length())) {
                // This is a 'color' term, output the annotation term with the payload information.
                termAtt.setEmpty().append(term);
                out.reset(payload.bytes);
                out.writeVInt(absTextPos);
                out.writeVInt(1); // For now we only recognize single-word colors.
                payload.length = out.getPosition();
                payloadAtt.setPayload(payload);
                return true;
            }
        }
        return false; // no more tokens
    }

    @Override
    public void reset() throws IOException {
        absTextPos = -1;
        super.reset();
    }

}