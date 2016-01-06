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

/** A {@link OneWordAnnotator} which accepts only colors. */
public class ColorAnnotator extends OneWordAnnotator {

    /** Default list of colors. */
    public static final String[] DEFAULT_COLORS = new String[] { "white", "navy", "blue", "aqua", "teal", "olive",
            "green", "lime", "yellow", "orange", "red", "maroon", "fuchsia", "purple", "silver", "gray", "black",
            "brown" };

    private static final ColorAnnotator DEFAULT = new ColorAnnotator(DEFAULT_COLORS);

    /** Returns a singleton {@link ColorAnnotator} which accepts only the {@link #DEFAULT_COLORS default colors}. */
    public static final ColorAnnotator withDefaultColors() {
        return DEFAULT;
    }

    public ColorAnnotator(String... colors) {
        super(colors);
    }

}