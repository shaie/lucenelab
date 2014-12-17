package com.shaie.annots;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.sinks.TeeSinkTokenFilter;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;

import com.shaie.Constants;
import com.shaie.spans.SpanInclusivePositionTermQuery;
import com.shaie.spans.SpanWithinQuery;

/** Demonstrates searching on an indexed annotation with a {@link SpanQuery}. */
public class AnnotationSearchExample {

    public static final String COLOR_ANNOT_TERM = "color";

    /** Prints the terms indexed under the given field. */
    static void printFieldTerms(AtomicReader reader, String field) throws IOException {
        System.out.println("Terms for field: " + field);
        TermsEnum te = reader.terms(field).iterator(null);
        BytesRef scratch;
        while ((scratch = te.next()) != null) {
            System.out.println("  " + scratch.utf8ToString());
        }
    }

    public static void main(String[] args) throws Exception {
        Directory dir = new RAMDirectory();
        IndexWriterConfig conf = new IndexWriterConfig(Constants.VERSTION, new WhitespaceAnalyzer());
        IndexWriter writer = new IndexWriter(dir, conf);

        // we need to add the annotation as a TokenStream field, therefore cannot use an Analyzer passed in the
        // IndexWriterConfig.
        Tokenizer tokenizer = new WhitespaceTokenizer(new StringReader("quick brown fox ate the blue red chicken"));
        TeeSinkTokenFilter textStream = new TeeSinkTokenFilter(tokenizer);
        TokenStream colorAnnotationStream = new AnnotatingTokenFilter(
                textStream.newSinkTokenStream(new ColorsSinkFilter()), COLOR_ANNOT_TERM);

        Document doc = new Document();
        doc.add(new TextField("text", textStream));
        doc.add(new TextField("annot", colorAnnotationStream));
        writer.addDocument(doc);

        writer.close();

        DirectoryReader reader = DirectoryReader.open(dir);
        AtomicReader ar = reader.leaves().get(0).reader(); // we only have one segment
        printFieldTerms(ar, "text");
        System.out.println();

        final ByteArrayDataInput in = new ByteArrayDataInput();
        DocsAndPositionsEnum dape = ar.termPositionsEnum(new Term("annot", COLOR_ANNOT_TERM));
        int docID = dape.nextDoc();
        int freq = dape.freq();
        System.out.println("Color annotation spans: doc=" + docID + ", freq=" + freq);
        for (int i = 0; i < freq; i++) {
            dape.nextPosition();
            BytesRef payload = dape.getPayload();
            in.reset(payload.bytes, payload.offset, payload.length);
            System.out.println("  start=" + in.readVInt() + ", length=" + in.readVInt());
        }

        IndexSearcher searcher = new IndexSearcher(reader);

        System.out.println("\nsearching for 'red WITHIN color':");
        Query q = new SpanWithinQuery(new SpanAnnotationTermQuery(new Term("annot", COLOR_ANNOT_TERM)),
                new SpanInclusivePositionTermQuery(new Term("text", "red")));
        TopDocs td = searcher.search(q, 10);
        System.out.println("  num results: " + td.scoreDocs.length);

        System.out.println("\nsearching for 'ate WITHIN color':");
        q = new SpanWithinQuery(new SpanAnnotationTermQuery(new Term("annot", COLOR_ANNOT_TERM)),
                new SpanInclusivePositionTermQuery(new Term("text", "ate")));
        td = searcher.search(q, 10);
        System.out.println("  num results: " + td.scoreDocs.length);

        reader.close();
        dir.close();
    }

}
