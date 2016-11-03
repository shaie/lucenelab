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

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.payloads.PayloadSpanCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.store.ByteArrayDataInput;

/**
 * A {@link SpanTermQuery} which returns a {@link Spans} whose {@link Spans#endPosition()} is read from a payload. This
 * allows to index one term which spans multiple positions.
 */
public class MultiPositionSpanTermQuery extends SpanTermQuery {

    private final PayloadSpanCollector payloadCollector = new PayloadSpanCollector();

    public MultiPositionSpanTermQuery(Term term) {
        super(term);
    }

    @Override
    public SpanWeight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
        final TermContext context;
        final IndexReaderContext topContext = searcher.getTopReaderContext();
        if (termContext == null || termContext.topReaderContext != topContext) {
            context = TermContext.build(topContext, term);
        } else {
            context = termContext;
        }
        return new SpanTermWeight(context, searcher, needsScores ? Collections.singletonMap(term, context) : null) {
            @Override
            public Spans getSpans(LeafReaderContext context, Postings requiredPostings) throws IOException {
                final Spans spans = super.getSpans(context, requiredPostings.atLeast(Postings.PAYLOADS));
                if (spans == null) { // term is not present in that reader
                    assert context.reader().docFreq(term) == 0 : "no term exists in reader term=" + term;
                    return null;
                }
                return new Spans() {

                    private int end = -1;
                    private final ByteArrayDataInput in = new ByteArrayDataInput();

                    @Override
                    public int advance(int target) throws IOException {
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
                            return NO_MORE_POSITIONS;
                        }
                        payloadCollector.reset();
                        collect(payloadCollector);
                        final byte[] payload = payloadCollector.getPayloads().iterator().next();
                        in.reset(payload);
                        end = in.readVInt() + pos;
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

}
