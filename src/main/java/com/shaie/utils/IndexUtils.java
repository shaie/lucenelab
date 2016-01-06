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
package com.shaie.utils;

import static com.shaie.utils.Utils.*;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BytesRef;

import com.google.common.collect.Maps;

public abstract class IndexUtils {

    private IndexUtils() {
        // No instances should be created.
    }

    /** Prints the terms indexed under the given fields. */
    public static void printFieldTerms(LeafReader reader, String... fields) throws IOException {
        for (final String field : fields) {
            System.out.println(format("Terms for field [%s]:", field));
            final TermsEnum te = reader.terms(field).iterator();
            BytesRef scratch;
            while ((scratch = te.next()) != null) {
                System.out.println(format("  %s", scratch.utf8ToString()));
            }
        }
    }

    /** Prints the terms indexed under the given fields with full postings information. */
    public static void printFieldTermsWithInfo(LeafReader reader, String... fields) throws IOException {
        for (final String field : fields) {
            System.out.println(format("Terms for field [%s], with positional info:", field));
            final TermsEnum te = reader.terms(field).iterator();
            BytesRef scratch;
            PostingsEnum postings = null;
            while ((scratch = te.next()) != null) {
                System.out.println(format("  %s", scratch.utf8ToString()));
                postings = te.postings(postings, PostingsEnum.ALL);
                for (postings.nextDoc(); postings.docID() != DocIdSetIterator.NO_MORE_DOCS; postings.nextDoc()) {
                    final Map<Integer, BytesRef> positions = Maps.newTreeMap();
                    boolean addedPayload = false;
                    for (int i = 0; i < postings.freq(); i++) {
                        final int pos = postings.nextPosition();
                        final BytesRef payload = postings.getPayload();
                        if (payload != null) {
                            positions.put(pos, BytesRef.deepCopyOf(payload));
                            addedPayload = true;
                        } else {
                            positions.put(pos, null);
                        }
                    }
                    if (addedPayload) {
                        System.out.println(format("    doc=%d, freq=%d", postings.docID(), postings.freq(), positions));
                        for (final Entry<Integer, BytesRef> e : positions.entrySet()) {
                            System.out.println(format("      pos=%d, payload=%s", e.getKey(), e.getValue()));
                        }
                    } else {
                        System.out.println(format("    doc=%d, freq=%d, pos=%s", postings.docID(), postings.freq(),
                                positions.keySet()));
                    }
                }
            }
        }
    }

}
