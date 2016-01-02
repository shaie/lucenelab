package com.shaie.annots;

import java.util.Arrays;
import java.util.Objects;

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

import org.apache.lucene.analysis.util.CharArraySet;

/** An {@link Annotator} which accepts only colors. */
public class ColorAnnotator implements Annotator {

    /** Default list of colors. */
    public static final String[] DEFAULT_COLORS = new String[] { "white", "navy", "blue", "aqua", "teal", "olive",
            "green", "lime", "yellow", "orange", "red", "maroon", "fuchsia", "purple", "silver", "gray", "black",
            "brown" };

    private static final ColorAnnotator DEFAULT = new ColorAnnotator(DEFAULT_COLORS);

    /** Returns a singleton {@link ColorAnnotator} which accepts only the {@link #DEFAULT_COLORS default colors}. */
    public static final ColorAnnotator withDefaultColors() {
        return DEFAULT;
    }

    private final CharArraySet colors;

    /** Uses the {@link #DEFAULT_COLORS default} list of colors. */
    public ColorAnnotator() {
        this(DEFAULT_COLORS);
    }

    public ColorAnnotator(String... colors) {
        Objects.requireNonNull(colors, "colors cannot be null");
        for (final String color : colors) {
            Objects.requireNonNull(color, "A color cannot be null");
        }
        this.colors = new CharArraySet(Arrays.asList(colors), true);
    }

    @Override
    public boolean accept(String text) {
        return colors.contains(text);
    }

    @Override
    public boolean accept(char[] text, int offset, int len) {
        return colors.contains(text, offset, len);
    }

}