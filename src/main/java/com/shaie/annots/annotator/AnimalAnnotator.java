package com.shaie.annots.annotator;

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

/** A {@link OneWordAnnotator} which accepts animals. */
public class AnimalAnnotator extends OneWordAnnotator {

    /** Default list of animals. */
    public static final String[] DEFAULT_ANIMALS = new String[] { "fox", "dog", "cat", "horse", "cow", "duck" };

    private static final AnimalAnnotator DEFAULT = new AnimalAnnotator(DEFAULT_ANIMALS);

    /** Returns a singleton {@link AnimalAnnotator} which accepts only the {@link #DEFAULT_ANIMALS default animals}. */
    public static final AnimalAnnotator withDefaultAnimals() {
        return DEFAULT;
    }

    public AnimalAnnotator(String... animals) {
        super(animals);
    }

}