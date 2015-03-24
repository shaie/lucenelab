package com.shaie;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;

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

public class PhraseVsSpanQuery {

    public static void main(String[] args) throws Exception {
        Directory dir = new RAMDirectory();
        IndexWriterConfig conf = new IndexWriterConfig(new WhitespaceAnalyzer());
        IndexWriter writer = new IndexWriter(dir, conf);

        Document doc = new Document();
        doc.add(new TextField("f", new TokenStream() {
            final PositionIncrementAttribute pos = addAttribute(PositionIncrementAttribute.class);
            final CharTermAttribute term = addAttribute(CharTermAttribute.class);
            boolean first = true, done = false;

            @Override
            public boolean incrementToken() throws IOException {
                if (done) {
                    return false;
                }
                if (first) {
                    term.setEmpty().append("a");
                    pos.setPositionIncrement(1);
                    first = false;
                } else {
                    term.setEmpty().append("b");
                    pos.setPositionIncrement(0);
                    done = true;
                }
                return true;
            }
        }));
        writer.addDocument(doc);
        writer.close();

        DirectoryReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);
        LeafReader ar = reader.leaves().get(0).reader();
        TermsEnum te = ar.terms("f").iterator(null);
        BytesRef scratch = new BytesRef();
        while ((scratch = te.next()) != null) {
            System.out.println(scratch.utf8ToString());
            DocsAndPositionsEnum dape = ar.termPositionsEnum(new Term("f", scratch.utf8ToString()));
            System.out.println("  doc=" + dape.nextDoc() + ", pos=" + dape.nextPosition());
        }

        System.out.println();

        // try a phrase query with a slop
        PhraseQuery pq = new PhraseQuery();
        pq.add(new Term("f", "a"));
        pq.add(new Term("f", "b"));

        System.out.println("searching for \"a b\"; num results = " + searcher.search(pq, 10).totalHits);

        pq.setSlop(1);
        System.out.println("searching for \"a b\"~1; num results = " + searcher.search(pq, 10).totalHits);

        pq.setSlop(3);
        System.out.println("searching for \"a b\"~3; num results = " + searcher.search(pq, 10).totalHits);

        SpanNearQuery snqUnOrdered = new SpanNearQuery(new SpanQuery[] { new SpanTermQuery(new Term("f", "a")),
                new SpanTermQuery(new Term("f", "b")) }, 1, false);
        System.out.println("searching for SpanNearUnordered('a', 'b'), slop=1; num results = "
                + searcher.search(snqUnOrdered, 10).totalHits);

        SpanNearQuery snqOrdered = new SpanNearQuery(new SpanQuery[] { new SpanTermQuery(new Term("f", "a")),
                new SpanTermQuery(new Term("f", "b")) }, 1, true);
        System.out.println("searching for SpanNearOrdered('a', 'b'), slop=1; num results = "
                + searcher.search(snqOrdered, 10).totalHits);

        reader.close();

        dir.close();
    }

}
