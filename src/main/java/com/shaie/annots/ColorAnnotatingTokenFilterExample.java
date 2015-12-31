package com.shaie.annots;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import com.google.common.collect.ImmutableMap;

/** Demonstrates indexing of a document with annotations. */
public class ColorAnnotatingTokenFilterExample {

    private static final String ANNOTS_FIELD = "annots";
    private static final String TEXT_FIELD = "text";
    public static final String COLOR_ANNOT_TERM = "color";
    public static final String TEXT = "quick brown fox and a red doc";

    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception {
        final Directory dir = new RAMDirectory();
        final Analyzer analyzer = createAnalyzer();
        final IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(analyzer));

        final Document doc = new Document();
        doc.add(new TextField(TEXT_FIELD, TEXT, Store.YES));
        doc.add(new TextField(ANNOTS_FIELD, TEXT, Store.NO));
        writer.addDocument(doc);
        writer.close();

        final DirectoryReader reader = DirectoryReader.open(dir);
        final LeafReader leaf = reader.leaves().get(0).reader(); // we only have one segment
        AnnotationsUtils.printAnnotations(leaf, new Term(ANNOTS_FIELD, COLOR_ANNOT_TERM));
        reader.close();
    }

    @SuppressWarnings("resource")
    private static Analyzer createAnalyzer() {
        final Analyzer annotsAnalyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                final Tokenizer tokenizer = new WhitespaceTokenizer();
                final TokenStream stream = new ColorAnnotatingTokenFilter(tokenizer, COLOR_ANNOT_TERM);
                return new TokenStreamComponents(tokenizer, stream);
            }
        };
        return new PerFieldAnalyzerWrapper(new WhitespaceAnalyzer(), ImmutableMap.of(ANNOTS_FIELD, annotsAnalyzer));
    }

}
