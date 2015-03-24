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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.util.Bits;

/**
 * A {@link SpanTermQuery} which returns a {@link Spans} whose {@link Spans#start()} and {@link Spans#end()} are read
 * from a payload, while the term's actual position is ignored.
 */
public class SpanAnnotationTermQuery extends SpanTermQuery {

    /**
     * Construct a {@link SpanAnnotationTermQuery} matching the given term's spans. The term is assumed to have
     * positions indexed with payload by {@link AnnotatingTokenFilter}, which records the start and end position of this
     * annotation.
     */
    public SpanAnnotationTermQuery(Term term) {
        super(term);
    }

    @Override
    public Spans getSpans(LeafReaderContext context, Bits acceptDocs, Map<Term, TermContext> termContexts)
            throws IOException {
        final Spans spans = super.getSpans(context, acceptDocs, termContexts);
        return new Spans() {
            private int start, end;
            final ByteArrayDataInput in = new ByteArrayDataInput();

            @Override
            public int start() {
                return start;
            }

            @Override
            public boolean skipTo(int target) throws IOException {
                return spans.skipTo(target);
            }

            @Override
            public boolean next() throws IOException {
                if (!spans.next()) {
                    return false;
                }
                if (!isPayloadAvailable()) {
                    return next();
                }
                byte[] payload = getPayload().iterator().next();
                in.reset(payload);
                start = in.readVInt();
                end = in.readVInt() + start - 1; // end is inclusive
                return true;
            }

            @Override
            public boolean isPayloadAvailable() throws IOException {
                return spans.isPayloadAvailable();
            }

            @Override
            public Collection<byte[]> getPayload() throws IOException {
                return spans.getPayload();
            }

            @Override
            public int end() {
                return end;
            }

            @Override
            public int doc() {
                return spans.doc();
            }

            @Override
            public long cost() {
                return spans.cost();
            }
        };
    }

}
