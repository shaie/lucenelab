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
package com.shaie.annots.filter;

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.store.ByteArrayDataOutput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.util.BytesRef;
import org.testng.collections.Lists;

/**
 * A {@link TokenFilter} which keeps only tokens with positions that are covered by a given array of annotation
 * positions and lengths. For example, if you process the text "quick brown fox and a red dog", and you give it the
 * array <code>[0,3,5,2]</code> (two annotations, {@code pos=0,len=3} and {@code pos=5,len=2}), then it will keep only
 * tokens: "quick", "brown", "fox", "red", "dog".
 * <p>
 * In addition, this filter outputs a special {@link #ANY_ANNOTATION_TERM} token, with its position set to the start of
 * the annotation and a {@link PayloadAttribute} with the annotation length, encoded as {@link DataOutput#writeVInt(int)
 * VInt}. So for the above example and annotations, it will output the token with {@code pos=0,payload=[3]} and
 * {@code pos=5,payload=[2]} .
 */
public final class PreAnnotatedTokenFilter extends TokenFilter {

    public static final String ANY_ANNOTATION_TERM = "_any_";

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final PayloadAttribute payloadAtt = addAttribute(PayloadAttribute.class);

    private final BytesRef payloadBytes = new BytesRef(5); // Maximum number of bytes for VInt
    private final ByteArrayDataOutput out = new ByteArrayDataOutput(payloadBytes.bytes);
    private final int[] markers;

    private int skippedPositions;
    private int absPosition;
    private int curStart;
    private int curEnd;
    private int pairIdx;
    private State state = null;

    public PreAnnotatedTokenFilter(TokenStream input, int... markers) {
        super(input);
        checkArgument(markers != null && markers.length > 0, "annotation markers cannot be null or empty");
        this.markers = getSortedFilteredMarkers(markers);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (state != null) {
            outputFirstAnnotatedTerm();
            return true;
        }

        skippedPositions = 0;
        while (input.incrementToken()) {
            final int posIncr = posIncrAtt.getPositionIncrement();
            absPosition += posIncr;
            if (acceptCurrentToken()) {
                posIncrAtt.setPositionIncrement(posIncr + skippedPositions);
                // Output the ANY_ANNOTATION_TERM term first
                if (absPosition == curStart) {
                    outputAnyTerm();
                }
                return true;
            }
            skippedPositions += posIncr;
        }
        return false;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        skippedPositions = 0;
        absPosition = -1;
        pairIdx = 0;
        updateCurrentStartEnd();
        state = null;
    }

    @Override
    public void end() throws IOException {
        super.end();
        posIncrAtt.setPositionIncrement(posIncrAtt.getPositionIncrement() + skippedPositions);
    }

    private void outputFirstAnnotatedTerm() {
        restoreState(state);
        state = null;
        // Output first annotated term at same position as ANY_ANNOTATION_TERM
        posIncrAtt.setPositionIncrement(0);
    }

    /** Update the payload attribute for the {@link #ANY_ANNOTATION_TERM}. */
    private void outputAnyTerm() throws IOException {
        state = captureState();
        termAtt.setEmpty().append(ANY_ANNOTATION_TERM);
        out.reset(payloadBytes.bytes);
        out.writeVInt(curEnd - curStart + 1);
        payloadBytes.length = out.getPosition();
        payloadAtt.setPayload(payloadBytes);
    }

    /** Is current token's position accepted by an annotation. */
    private boolean acceptCurrentToken() {
        if (absPosition < curStart) {
            return false;
        }
        if (absPosition > curEnd) {
            pairIdx += 2;
            if (pairIdx < markers.length) {
                updateCurrentStartEnd();
                // Check if next annotation accepts it.
                return acceptCurrentToken();
            }
            // No more annotated tokens
            curStart = Integer.MAX_VALUE;
            curEnd = Integer.MAX_VALUE;
            return false;
        }
        return true;
    }

    /** Update current start and end positions. */
    private void updateCurrentStartEnd() {
        curStart = markers[pairIdx];
        curEnd = curStart + markers[pairIdx + 1] - 1;
    }

    /**
     * Returns a sorted list of annotation pairs (pos + len), and also filters annotation infos that are covered by
     * others. E.g. the annotation info [5,1] is covered by the annotation [4,3] and therefore is redundant to keep.
     */
    private static int[] getSortedFilteredMarkers(int... annotations) {
        if (annotations == null || annotations.length == 0) {
            return new int[0];
        }
        if ((annotations.length & 0x1) != 0) {
            throw new IllegalArgumentException("expected even number of integers, got " + annotations.length);
        }

        final int[][] sorted = getSortedMarkers(annotations);
        final List<Integer> filtered = Lists.newArrayList();
        filtered.add(sorted[0][0]);
        filtered.add(sorted[0][1]);
        int start = sorted[0][0];
        int end = start + sorted[0][1] - 1;
        for (int i = 1; i < sorted.length; i++) {
            final int thisStart = sorted[i][0];
            final int thisEnd = thisStart + sorted[i][1] - 1;
            if (thisEnd <= end) {
                // Filter that annotation information since it's covered in the previous one.
                continue;
            }
            filtered.add(sorted[i][0]);
            filtered.add(sorted[i][1]);
            start = thisStart;
            end = thisEnd;
        }

        final int[] result = new int[filtered.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = filtered.get(i);
        }
        return result;
    }

    private static int[][] getSortedMarkers(int... annotations) {
        final int[][] annots = new int[annotations.length / 2][2];
        for (int i = 0, j = 0; i < annotations.length; i += 2, j++) {
            annots[j][0] = annotations[i];
            annots[j][1] = annotations[i + 1];
        }
        Arrays.sort(annots, new Comparator<int[]>() {
            @Override
            public int compare(int[] o1, int[] o2) {
                if (o1[0] != o2[0]) {
                    // Start position is not the same, smaller one comes first.
                    return o1[0] - o2[0];
                }

                // Both start at the same position, longer one comes first.
                return o2[1] - o1[1];
            }
        });
        return annots;
    }

}