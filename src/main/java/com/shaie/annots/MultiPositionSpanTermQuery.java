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
package com.shaie.annots;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.util.BytesRef;

/**
 * A {@link SpanTermQuery} which returns a {@link Spans} whose {@link Spans#endPosition()} is read from a payload. This
 * allows to index one term which spans multiple positions.
 */
public class MultiPositionSpanTermQuery extends SpanTermQuery {

    public MultiPositionSpanTermQuery(Term term) {
        super(term);
    }

    @Override
    public SpanWeight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
        final IndexReaderContext topContext = searcher.getTopReaderContext();
        final TermContext context;
        if (termContext == null || termContext.wasBuiltFor(topContext) == false) {
            context = TermContext.build(topContext, term);
        } else {
            context = termContext;
        }
        final Map<Term, TermContext> terms = needsScores ? Collections.singletonMap(term, context) : null;
        return new SpanTermWeight(context, searcher, terms, boost) {
            @Override
            public Spans getSpans(LeafReaderContext context, Postings requiredPostings) throws IOException {
                final TermSpans spans =
                        (TermSpans) super.getSpans(context, requiredPostings.atLeast(Postings.PAYLOADS));
                if (spans == null) { // term is not present in that reader
                    assert context.reader().docFreq(term) == 0 : "no term exists in reader term=" + term;
                    return null;
                }
                return new Spans() {

                    private final PositionSpansCollector payloadCollector = new PositionSpansCollector();
                    private int end = -1;

                    @Override
                    public int advance(int target) throws IOException {
                        end = -1;
                        return spans.advance(target);
                    }

                    @Override
                    public void collect(SpanCollector collector) throws IOException {
                        spans.collect(collector);
                    }

                    @Override
                    public long cost() {
                        return spans.cost();
                    }

                    @Override
                    public int docID() {
                        return spans.docID();
                    }

                    @Override
                    public int endPosition() {
                        return end;
                    }

                    @Override
                    public int nextDoc() throws IOException {
                        end = -1;
                        return spans.nextDoc();
                    }

                    @Override
                    public int nextStartPosition() throws IOException {
                        final int pos = spans.nextStartPosition();
                        if (pos == NO_MORE_POSITIONS) {
                            end = NO_MORE_POSITIONS;
                            return NO_MORE_POSITIONS;
                        }
                        spans.collect(payloadCollector);
                        end = payloadCollector.payloadValue + pos;
                        return pos;
                    }

                    @Override
                    public float positionsCost() {
                        return spans.positionsCost();
                    }

                    @Override
                    public int startPosition() {
                        return spans.startPosition();
                    }

                    @Override
                    public int width() {
                        return spans.width();
                    }
                };
            }
        };
    }

    @Override
    public String toString(String field) {
        return "mspans(" + super.toString(field) + ")";
    }

    private static class PositionSpansCollector implements SpanCollector {

        private final ByteArrayDataInput in = new ByteArrayDataInput();
        int payloadValue = -1;

        @Override
        public void collectLeaf(PostingsEnum postings, int position, Term term) throws IOException {
            final BytesRef payload = postings.getPayload();
            in.reset(payload.bytes, payload.offset, payload.length);
            payloadValue = in.readVInt();
        }

        @Override
        public void reset() {
            payloadValue = -1;
        }
    }

}
