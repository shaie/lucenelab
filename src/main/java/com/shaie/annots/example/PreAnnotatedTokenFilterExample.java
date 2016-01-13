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
package com.shaie.annots.example;

import static com.shaie.annots.filter.PreAnnotatedTokenFilter.*;
import static com.shaie.utils.Utils.*;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.sinks.TeeSinkTokenFilter;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.FieldMaskingSpanQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import com.shaie.annots.MultiPositionSpanTermQuery;
import com.shaie.annots.filter.PreAnnotatedTokenFilter;
import com.shaie.utils.IndexUtils;

/**
 * Demonstrates indexing of documents with pre-annotated text markers.
 */
public class PreAnnotatedTokenFilterExample {

    private static final String COLOR_FIELD = "color";
    private static final String TEXT_FIELD = "text";

    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception {
        final Directory dir = new RAMDirectory();
        final Analyzer analyzer = new WhitespaceAnalyzer();
        final IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        final IndexWriter writer = new IndexWriter(dir, conf);

        addDocument(writer, "quick rosy brown fox and a pale violet red dog", 1, 2, 2, 1, 6, 3, 7, 1, 8, 1);
        addDocument(writer, "only red dog", 1, 1);
        addDocument(writer, "man with red pale face", 2, 1);
        writer.close();

        final QueryParser qp = new QueryParser(TEXT_FIELD, analyzer);
        qp.setAllowLeadingWildcard(true);

        final DirectoryReader reader = DirectoryReader.open(dir);
        final LeafReader leaf = reader.leaves().get(0).reader(); // We only have one segment
        IndexUtils.printFieldTerms(leaf, TEXT_FIELD, COLOR_FIELD);
        IndexUtils.printFieldTermsWithInfo(leaf, COLOR_FIELD);
        System.out.println();

        final IndexSearcher searcher = new IndexSearcher(reader);

        search(searcher, qp.parse("color:" + ANY_ANNOTATION_TERM));
        System.out.println();

        search(searcher, qp.parse("color:pale"));
        System.out.println();

        searchForColoredFox(searcher);
        System.out.println();

        reader.close();
    }

    @SuppressWarnings("resource")
    private static void addDocument(IndexWriter writer, String text, int... colorAnnotations) throws IOException {
        final Tokenizer tokenizer = new WhitespaceTokenizer();
        tokenizer.setReader(new StringReader(text));
        final TeeSinkTokenFilter textStream = new TeeSinkTokenFilter(tokenizer);
        final TokenStream colorsStream =
                new PreAnnotatedTokenFilter(textStream.newSinkTokenStream(), colorAnnotations);

        final Document doc = new Document();
        doc.add(new StoredField(TEXT_FIELD, text));
        doc.add(new TextField(TEXT_FIELD, textStream));
        doc.add(new TextField(COLOR_FIELD, colorsStream));
        writer.addDocument(doc);
    }

    private static void searchForColoredFox(IndexSearcher searcher) throws IOException {
        final SpanQuery anyColor = new MultiPositionSpanTermQuery(new Term(COLOR_FIELD, ANY_ANNOTATION_TERM));
        final SpanQuery colorAsText = new FieldMaskingSpanQuery(anyColor, TEXT_FIELD);
        final SpanQuery fox = new SpanTermQuery(new Term(TEXT_FIELD, "fox"));
        final SpanQuery coloredFox = new SpanNearQuery(new SpanQuery[] { colorAsText, fox }, 0, true);
        search(searcher, coloredFox);
    }

    private static void search(IndexSearcher searcher, Query q) throws IOException {
        System.out.println(format("Searching for [%s]:", q));
        final TopDocs results = searcher.search(q, 10);
        for (final ScoreDoc sd : results.scoreDocs) {
            System.out.println(format("  doc=%d, text=%s", sd.doc, searcher.doc(sd.doc).get(TEXT_FIELD)));
        }
    }

}