package com.shaie.annots;

import java.io.IOException;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.ByteArrayDataInput;
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

public abstract class AnnotationsUtils {

    private AnnotationsUtils() {
        // No instances should be created.
    }

    public static void printAnnotations(LeafReader reader, Term term) throws IOException {
        System.out.println("Annotations for " + term);
        final ByteArrayDataInput in = new ByteArrayDataInput();
        final PostingsEnum postings = reader.postings(term, PostingsEnum.PAYLOADS);
        for (int docID = postings.nextDoc(); docID != DocIdSetIterator.NO_MORE_DOCS; docID = postings.nextDoc()) {
            final int freq = postings.freq();
            System.out.println("  doc=" + docID + ", freq=" + freq);
            for (int i = 0; i < freq; i++) {
                postings.nextPosition();
                final BytesRef payload = postings.getPayload();
                in.reset(payload.bytes, payload.offset, payload.length);
                System.out.println("    start=" + in.readVInt() + ", length=" + in.readVInt());
            }
        }
    }

}
