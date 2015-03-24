package com.shaie.suggest;

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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.Dictionary;
import org.apache.lucene.search.suggest.DocumentDictionary;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;

/**
 * Demonstrates how to use context in suggestions, using AnalyzingInfixSuggester. This suggester builds a suggestions
 * sidecar index, from which it provides suggestions.
 *
 * <p>
 * This demo first builds a search index, from which the suggestion index is later created, using
 * {@link DocumentDictionary}. This is a convenient and easy way to build the suggester's sidecar index, however not the
 * only one. You can use {@link AnalyzingInfixSuggester#build(Dictionary)} or
 * {@link AnalyzingInfixSuggester#build(org.apache.lucene.search.suggest.InputIterator)} directly, if the dictionary for
 * suggestions exists elsewhere (e.g. in a separate file).
 * </p>
 */
public class ContextSuggestDemo {

    private static final BytesRef USER1_CONTEXT = new BytesRef("user1");
    private static final BytesRef USER1_TEXT = new BytesRef("quick brown fox");

    private static final BytesRef USER2_CONTEXT = new BytesRef("user2");
    private static final BytesRef USER2_TEXT = new BytesRef("quick blue fox");

    private final Directory indexDir; // the directory where documents are indexed
    private final Directory suggestDir; // the directory used by the suggester to store and provide sugegstions
    private final Analyzer analyzer;
    private final AnalyzingInfixSuggester suggester;

    public ContextSuggestDemo() throws IOException {
        indexDir = new RAMDirectory();
        suggestDir = new RAMDirectory();
        analyzer = new SimpleAnalyzer();
        suggester = new AnalyzingInfixSuggester(suggestDir, analyzer, analyzer, 1, true);
        buildSearchIndex();
        buildSuggesterIndex();
    }

    private void buildSearchIndex() throws IOException {
        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        try (IndexWriter writer = new IndexWriter(indexDir, conf)) {
            Document doc = new Document();
            doc.add(new StringField("username", USER1_CONTEXT.utf8ToString(), Store.YES));
            doc.add(new TextField("content", USER1_TEXT.utf8ToString(), Store.YES));
            writer.addDocument(doc);

            doc = new Document();
            doc.add(new StringField("username", USER2_CONTEXT.utf8ToString(), Store.YES));
            doc.add(new TextField("content", USER2_TEXT.utf8ToString(), Store.YES));
            writer.addDocument(doc);
        }
    }

    private void buildSuggesterIndex() throws IOException {
        try (DirectoryReader reader = DirectoryReader.open(indexDir)) {
            Dictionary dictionary = new DocumentDictionary(reader, "content", null, null, "username");
            suggester.build(dictionary);
            suggester.refresh();
        }
    }

    public void lookupNoContext() throws IOException {
        System.out.println("Running lookup() with no context:");
        List<LookupResult> lookups = suggester.lookup("qu", (Set<BytesRef>) null, 10, true, true);
        for (LookupResult lookup : lookups) {
            System.out.println(lookup);
        }
        System.out.println();
    }

    public void lookupWithContext(BytesRef context) throws IOException {
        System.out.println("Running lookup() with context [" + context.utf8ToString() + "]:");
        List<LookupResult> lookups = suggester.lookup("qu", Collections.singleton(context), 10, true, true);
        for (LookupResult lookup : lookups) {
            System.out.println(lookup);
        }
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        ContextSuggestDemo suggestDemo = new ContextSuggestDemo();

        suggestDemo.lookupNoContext();
        suggestDemo.lookupWithContext(USER1_CONTEXT);
        suggestDemo.lookupWithContext(USER2_CONTEXT);
    }

}