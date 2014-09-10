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

import org.apache.lucene.util.AttributeImpl;

/** An {@link AnnotationSpanAttribute} implementation. */
public final class AnnotationSpanAttributeImpl extends AttributeImpl implements AnnotationSpanAttribute {

  private int start, length;
  
  @Override
  public void setSpan(int start, int length) {
    this.start = start;
    this.length = length;
  }

  @Override
  public int getStart() {
    return start;
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public void clear() {
    start = length = -1;
  }

  @Override
  public void copyTo(AttributeImpl target) {
    ((AnnotationSpanAttributeImpl) target).setSpan(start, length);
  }
  
}