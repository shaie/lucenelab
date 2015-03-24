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

import java.util.Arrays;

import org.apache.lucene.analysis.sinks.TeeSinkTokenFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.AttributeSource;

/**
 * A {@link org.apache.lucene.analysis.sinks.TeeSinkTokenFilter.SinkFilter} which accepts colors. It stores on the
 * {@link AttributeSource} an {@link AnnotationSpanAttribute} recording the start position (in the original text) and
 * the length (usually 1) of the identified color.
 */
public final class ColorsSinkFilter extends TeeSinkTokenFilter.SinkFilter {

    private static final CharArraySet COLORS = new CharArraySet(
            Arrays.asList("black", "blue", "brown", "red", "green"), true);

    private CharTermAttribute termAtt = null;
    private AnnotationSpanAttribute annotSpanAtt = null;
    private PositionIncrementAttribute posIncrAtt = null;

    private int absTextPos = -1;

    @Override
    public boolean accept(AttributeSource source) {
        if (termAtt == null) {
            termAtt = source.getAttribute(CharTermAttribute.class);
            posIncrAtt = source.addAttribute(PositionIncrementAttribute.class);
            annotSpanAtt = source.addAttribute(AnnotationSpanAttribute.class);
        }

        // NOTE: the state of the input AttributeSource is not cloned before
        // calling this method and thus shared with other consumers of that
        // source. Therefore we avoid modifying any existing attributes, and add
        // on the stream a special attribute that will be passed on to the
        // TokenFilter which consumes the color terms.

        absTextPos += posIncrAtt.getPositionIncrement(); // adjust the absolute position in the text
        boolean isColor = COLORS.contains(termAtt.buffer(), 0, termAtt.length());
        if (isColor) {
            // System.out.println("found color: " + termAtt + ", pos=" + absTextPos);
            annotSpanAtt.setSpan(absTextPos, 1);
        }

        return isColor;
    }

}