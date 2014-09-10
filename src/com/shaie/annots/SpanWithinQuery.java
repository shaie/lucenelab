package com.shaie.annots;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

/**
 * A {@link SpanQuery} which matches documents which has a 'match' clause within
 * a 'range' clause.
 */
public class SpanWithinQuery extends SpanQuery {

  private SpanQuery range, match;

  public SpanWithinQuery(SpanQuery range, SpanQuery match) {
    this.range = range;
    this.match = match;
  }

  /** Returns true iff <code>o</code> is equal to this. */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof SpanWithinQuery)) {
      return false;
    }

    SpanWithinQuery other = (SpanWithinQuery) o;

    return this.range.equals(other.range) && this.match.equals(other.match) && (this.getBoost() == other.getBoost());
  }

  @Override
  public String getField() {
    return match.getField();
  }

  @Override
  public Spans getSpans(final AtomicReaderContext context, final Bits acceptDocs,
      final Map<Term,TermContext> termContexts) throws IOException {
    final Spans rangeSpans = range.getSpans(context, acceptDocs, termContexts);
    final Spans matchSpans = match.getSpans(context, acceptDocs, termContexts);
    return new Spans() {
      private boolean moreRange = true, moreMatch = true;
      
      @Override
      public long cost() {
        return rangeSpans.cost();
      }

      @Override
      public int doc() {
        return rangeSpans.doc();
      }

      @Override
      public int end() {
        return rangeSpans.end();
      }

      @Override
      public Collection<byte[]> getPayload() throws IOException {
        return rangeSpans.getPayload();
      }

      @Override
      public boolean isPayloadAvailable() throws IOException {
        return rangeSpans.isPayloadAvailable();
      }

      @Override
      public boolean next() throws IOException {
        if (moreRange) {
          moreRange = rangeSpans.next();
        }
        
        while (moreRange && moreMatch) {
          // move both range and match spans to the same document
          while (rangeSpans.doc() != matchSpans.doc()) {
            if (rangeSpans.doc() > matchSpans.doc()) {
              moreMatch = matchSpans.skipTo(rangeSpans.doc());
              if (!moreMatch)
                return false;
            } else {
              moreRange = rangeSpans.skipTo(matchSpans.doc());
              if (!moreRange)
                return false;
            }
          }

          // range and match spans are on the same doc, check that the spans intersect
          if (matchSpans.start() >= rangeSpans.start() && matchSpans.end() <= rangeSpans.end()) {
            return true;
          } else {
            // spans do not intersect, move to the next of either and look for the next match
            if (rangeSpans.end() < matchSpans.start()) {
              moreRange = rangeSpans.next(); // try the next range span
            } else {
              moreMatch = matchSpans.next(); // try the next match span
            }
          }
        }

        return moreRange && moreMatch;
      }

      @Override
      public boolean skipTo(int target) throws IOException {
        if (!matchSpans.skipTo(target) || !rangeSpans.skipTo(target)) {
          return false; // either one or both spans don't have a match on or after target
        }
        
        // both matchSpans and rangeSpans have a document on or after 'target'
        return next();
      }

      @Override
      public int start() {
        return rangeSpans.start();
      }

      @Override
      public String toString() {
        return "spans(" + SpanWithinQuery.this.toString() + ")";
      }
    };
  }

  @Override
  public int hashCode() {
    int h = range.hashCode();
    h = (h << 1) | (h >>> 31); // rotate left
    h ^= match.hashCode();
    h = (h << 1) | (h >>> 31); // rotate left
    h ^= Float.floatToRawIntBits(getBoost());
    return h;
  }
  
  @Override
  public void extractTerms(Set<Term> terms) {
    match.extractTerms(terms);
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    SpanWithinQuery clone = null;

    SpanQuery rewrittenRange = (SpanQuery) range.rewrite(reader);
    if (rewrittenRange != range) {
      clone = (SpanWithinQuery) this.clone();
      clone.range = rewrittenRange;
    }

    SpanQuery rewrittenMatch = (SpanQuery) match.rewrite(reader);
    if (rewrittenMatch != match) {
      if (clone == null) {
        clone = (SpanWithinQuery) this.clone();
      }
      clone.match = rewrittenMatch;
    }

    if (clone != null) {
      return clone; // some clauses rewrote
    } else {
      return this; // no clauses rewrote
    }
  }

  @Override
  public String toString(String field) {
    StringBuilder sb = new StringBuilder();
    sb.append("spanWithin(");
    sb.append(range.toString(field));
    sb.append(", ");
    sb.append(match.toString(field));
    sb.append(")");
    sb.append(ToStringUtils.boost(getBoost()));
    return sb.toString();
  }

}
