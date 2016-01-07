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
package com.shaie.annots.filter;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

/** A {@link TokenFilter} which emits the token {@link #ANY_ANNOTATION_TERM} for every term that it encounters. */
public final class AnyAnnotationTokenFilter extends TokenFilter {

    public static final String ANY_ANNOTATION_TERM = "_any_";

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

    private boolean addedOrigTerm = false;

    public AnyAnnotationTokenFilter(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!addedOrigTerm) {
            addedOrigTerm = true;
            return input.incrementToken();
        }

        termAtt.setEmpty().append(ANY_ANNOTATION_TERM);
        posIncrAtt.setPositionIncrement(0); // Add this term at the same position
        addedOrigTerm = false;
        return true;
    }

    @Override
    public void reset() throws IOException {
        addedOrigTerm = false;
        super.reset();
    }

}