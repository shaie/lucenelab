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
package com.shaie.facet;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/** Demonstrates how to restrict a search to not match documents with a specific facet. */
public class NotDrillDownExample {

    private static final String AUTHOR_FACET = "Author";

    private static final Directory indexDir = new RAMDirectory();
    private static final Directory taxoDir = new RAMDirectory();
    private static final FacetsConfig config = new FacetsConfig();

    public static void main(String[] args) throws Exception {
        createIndex();

        try (DirectoryReader indexReader = DirectoryReader.open(indexDir);
                TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);) {
            final IndexSearcher searcher = new IndexSearcher(indexReader);

            // Find the index field which holds the 'Author' facets
            final String indexedField = config.getDimConfig(AUTHOR_FACET).indexFieldName;
            final Query q = new BooleanQuery.Builder()
                    // Here you would usually use a different query
                    .add(new MatchAllDocsQuery(), Occur.MUST)
                    // Exclude results with Author/Lisa
                    .add(new TermQuery(DrillDownQuery.term(indexedField, AUTHOR_FACET, "Lisa")), Occur.MUST_NOT)
                    .build();

            final TopDocs topDocs = searcher.search(q, 10);
            assert topDocs.scoreDocs.length == 1 : "should have found 1 document with Author/Bob";
            final Document doc = searcher.doc(topDocs.scoreDocs[0].doc);
            System.out.println(doc);
        }
    }

    private static void createIndex() throws IOException {
        try (Analyzer analyzer = new WhitespaceAnalyzer();
                IndexWriter indexWriter = new IndexWriter(indexDir, new IndexWriterConfig(analyzer));
                TaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir)) {
            for (final String author : new String[] { "Bob", "Lisa" }) {
                final Document doc = new Document();
                doc.add(new FacetField(AUTHOR_FACET, author));
                doc.add(new StoredField(AUTHOR_FACET, author));
                indexWriter.addDocument(config.build(taxoWriter, doc));
            }
        }
    }

}
