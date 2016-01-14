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
package com.shaie.fst;

import java.io.PrintWriter;

import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.lucene.util.fst.Util;

public class FstExample {

    public static void main(String[] args) throws Exception {
        final CharsRef output = new CharsRef("color");
        final SynonymMap.Builder builder = new SynonymMap.Builder(true);
        builder.add(SynonymMap.Builder.join("blue".split(" "), new CharsRefBuilder()), output, true);
        builder.add(SynonymMap.Builder.join("green".split(" "), new CharsRefBuilder()), output, true);
        builder.add(SynonymMap.Builder.join("pale green".split(" "), new CharsRefBuilder()), output, true);
        builder.add(SynonymMap.Builder.join("pale blue".split(" "), new CharsRefBuilder()), output, true);
        builder.add(SynonymMap.Builder.join("dark sea green".split(" "), new CharsRefBuilder()), output, true);
        final SynonymMap synMap = builder.build();
        try (PrintWriter pw = new PrintWriter("d:/tmp/syns.dot");) {
            Util.toDot(synMap.fst, pw, true, true);
        }
        System.out.println("Done!");
    }

}