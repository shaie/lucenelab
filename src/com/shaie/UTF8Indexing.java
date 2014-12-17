package com.shaie;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

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

public class UTF8Indexing {

    private static void search(IndexSearcher searcher, QueryParser qp, String text) throws Exception {
        System.out.println("search for [" + text + "]: " + searcher.search(qp.parse(text), 10).totalHits);
    }

    public static void main(String[] args) throws Exception {
        Directory dir = new RAMDirectory();
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig conf = new IndexWriterConfig(Constants.VERSTION, analyzer);
        IndexWriter writer = new IndexWriter(dir, conf);

        Document doc = new Document();
        doc.add(new TextField("f", "Russia\u2013United States relations", Store.YES));
        writer.addDocument(doc);
        writer.close();

        DirectoryReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);
        QueryParser qp = new QueryParser("f", analyzer);
        search(searcher, qp, "Russia United States relations");
        search(searcher, qp, "\"Russia United states relations\"");
        search(searcher, qp, "\"Russia-United states relations\"");
        search(searcher, qp, "\"Russia\u2013United states relations\"");
        reader.close();

        dir.close();
    }

}
