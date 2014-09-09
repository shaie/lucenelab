package com.shaie.annotts;

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

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.store.ByteArrayDataOutput;
import org.apache.lucene.util.BytesRef;

/**
 * A {@link TokenFilter} which returns a single color annotation
 * {@link ColorAnnotatingTokenFilter#COLOR_ANNOT_TERM term}, with a
 * {@link PayloadAttribute} which records the start position of the annotation
 * (in the original text) as well as its length).
 */
public final class ColorAnnotatingTokenFilter extends TokenFilter {

  public static final String COLOR_ANNOT_TERM = "color";
  
  private final CharTermAttribute termAtt = getAttribute(CharTermAttribute.class);
  private final AnnotationSpanAttribute annotSpanAtt = addAttribute(AnnotationSpanAttribute.class);
  private final PayloadAttribute payloadAtt = addAttribute(PayloadAttribute.class);
  private final BytesRef payload = new BytesRef(10); // max 5 bytes per VInt
  private final ByteArrayDataOutput out = new ByteArrayDataOutput();
  
  /** Sole constructor. */
  public ColorAnnotatingTokenFilter(TokenStream input) {
    super(input);
    payloadAtt.setPayload(payload);
  }
  
  @Override
  public boolean incrementToken() throws IOException {
    if (!input.incrementToken()) return false; // no more tokens
    
    // update the payload with the annotation span.
    termAtt.setEmpty().append(COLOR_ANNOT_TERM);
    out.reset(payload.bytes);
    out.writeVInt(annotSpanAtt.getStart());
    out.writeVInt(annotSpanAtt.getLength());
    payload.length = out.getPosition();
    return true;
  }
  
}