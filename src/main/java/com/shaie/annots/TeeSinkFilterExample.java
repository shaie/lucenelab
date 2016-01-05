package com.shaie.annots;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.sinks.TeeSinkTokenFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;

public class TeeSinkFilterExample {

    @SuppressWarnings("resource")
    public static void main(String[] args) throws IOException {
        final Tokenizer tok = new WhitespaceTokenizer();
        tok.setReader(new StringReader("one two three four five six"));
        final TeeSinkTokenFilter tee = new TeeSinkTokenFilter(tok);
        final TokenStream sink1 = new TokenFilter(tee.newSinkTokenStream()) {
            private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

            @Override
            public boolean incrementToken() throws IOException {
                if (!input.incrementToken()) {
                    return false;
                }

                posIncrAtt.setPositionIncrement(0);
                return true;
            }
        };
        final TokenStream sink2 = new FilteringTokenFilter(tee.newSinkTokenStream()) {
            private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

            @Override
            protected boolean accept() throws IOException {
                final String term = termAtt.toString();
                return !term.equals("one") && !term.equals("three");
            }
        };
        System.out.println("tee");
        printStream(tee);
        System.out.println();
        System.out.println("sink1");
        printStream(sink1);
        System.out.println();
        System.out.println("sink2");
        printStream(sink2);
    }

    private static void printStream(TokenStream ts) throws IOException {
        final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
        final PositionIncrementAttribute posIncrAtt = ts.addAttribute(PositionIncrementAttribute.class);
        int pos = -1;
        ts.reset();
        while (ts.incrementToken()) {
            pos += posIncrAtt.getPositionIncrement();
            System.out.println("term=" + termAtt + ", pos=" + pos);
        }
        ts.end();
    }

}