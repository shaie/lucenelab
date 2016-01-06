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
package com.shaie.annots;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;

import com.shaie.annots.annotator.Annotator;

/** A {@link FilteringTokenFilter} which uses an {@link Annotator} to {@link #accept()} tokens. */
public final class AnnotatorTokenFilter extends FilteringTokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final Annotator annotator;

    public AnnotatorTokenFilter(TokenStream input, Annotator annotator) {
        super(input);
        this.annotator = annotator;
    }

    @Override
    protected boolean accept() throws IOException {
        return annotator.accept(termAtt.buffer(), 0, termAtt.length());
    }

}
