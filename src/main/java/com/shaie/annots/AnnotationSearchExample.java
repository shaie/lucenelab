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
 * Unless required byOCP applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.StringReader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.sinks.TeeSinkTokenFilter;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.FieldMaskingSpanQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanWithinQuery;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;

import com.shaie.utils.IndexUtils;

/** Demonstrates searching on an indexed annotation with a {@link SpanQuery}. */
public class AnnotationSearchExample {

    private static final String ANNOT_FIELD = "annot";
    private static final String TEXT_FIELD = "text";
    public static final String COLOR_ANNOT_TERM = "color";

    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception {
        final Directory dir = new RAMDirectory();
        final IndexWriterConfig conf = new IndexWriterConfig(new WhitespaceAnalyzer());
        final IndexWriter writer = new IndexWriter(dir, conf);

        // we need to add the annotation as a TokenStream field, therefore cannot use an Analyzer passed to
        // IndexWriterConfig.
        final Tokenizer tokenizer = new WhitespaceTokenizer();
        tokenizer.setReader(new StringReader("quick brown fox ate the blue red chicken"));
        final TeeSinkTokenFilter textStream = new TeeSinkTokenFilter(tokenizer);
        final TokenStream colorAnnotationStream = new AnnotatingTokenFilter(
                textStream.newSinkTokenStream(new ColorsSinkFilter()), COLOR_ANNOT_TERM);

        final Document doc = new Document();
        doc.add(new TextField(TEXT_FIELD, textStream));
        doc.add(new TextField(ANNOT_FIELD, colorAnnotationStream));
        writer.addDocument(doc);

        writer.close();

        final DirectoryReader reader = DirectoryReader.open(dir);
        final LeafReader leaf = reader.leaves().get(0).reader(); // we only have one segment
        IndexUtils.printFieldTerms(leaf, TEXT_FIELD);
        System.out.println();

        final ByteArrayDataInput in = new ByteArrayDataInput();
        final PostingsEnum dape = leaf.postings(new Term(ANNOT_FIELD, COLOR_ANNOT_TERM), PostingsEnum.PAYLOADS);
        final int docID = dape.nextDoc();
        final int freq = dape.freq();
        System.out.println("Color annotation spans: doc=" + docID + ", freq=" + freq);
        for (int i = 0; i < freq; i++) {
            dape.nextPosition();
            final BytesRef payload = dape.getPayload();
            in.reset(payload.bytes, payload.offset, payload.length);
            System.out.println("  start=" + in.readVInt() + ", length=" + in.readVInt());
        }

        final IndexSearcher searcher = new IndexSearcher(reader);

        System.out.println("\nsearching for 'red WITHIN color':");
        final Query redWithinColor = new org.apache.lucene.search.spans.SpanWithinQuery(
                new FieldMaskingSpanQuery(new SpanAnnotationTermQuery(new Term(ANNOT_FIELD, COLOR_ANNOT_TERM)),
                        TEXT_FIELD),
                new SpanTermQuery(new Term(TEXT_FIELD, "red")));
        final TopDocs redWithinColorTopDocs = searcher.search(redWithinColor, 10);
        System.out.println(" num results: " + redWithinColorTopDocs.scoreDocs.length);

        System.out.println("\nsearching for 'ate WITHIN color':");
        final Query ateWithinColor = new SpanWithinQuery(
                new FieldMaskingSpanQuery(new SpanAnnotationTermQuery(new Term(ANNOT_FIELD, COLOR_ANNOT_TERM)),
                        TEXT_FIELD),
                new SpanTermQuery(new Term(TEXT_FIELD, "ate")));
        final TopDocs ateWithinColorTopDocs = searcher.search(ateWithinColor, 10);
        System.out.println(" num results: " + ateWithinColorTopDocs.scoreDocs.length);

        reader.close();
        dir.close();
    }

}
