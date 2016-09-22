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
package com.shaie.annots.annotator;

import java.util.Arrays;
import java.util.Objects;

import org.apache.lucene.analysis.CharArraySet;

/** An {@link Annotator} which accepts single words. */
public class OneWordAnnotator implements Annotator {

    private final CharArraySet words;

    public OneWordAnnotator(String... words) {
        Objects.requireNonNull(words, "words cannot be null");
        for (final String word : words) {
            Objects.requireNonNull(word, "Word cannot be null");
        }
        this.words = new CharArraySet(Arrays.asList(words), true);
    }

    @Override
    public final boolean accept(String text) {
        return words.contains(text);
    }

    @Override
    public final boolean accept(char[] text, int offset, int len) {
        return words.contains(text, offset, len);
    }

}