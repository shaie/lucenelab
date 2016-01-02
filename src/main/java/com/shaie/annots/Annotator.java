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
 * Unless required byOCP applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** An interface for annotating text. */
public interface Annotator {

    /** Returns true iff the annotator accepts the given text, false otherwise. */
    public boolean accept(String text);

    /**
     * Returns true iff the annotator accepts the given text, false otherwise. The text is read from the input array at
     * start {@code offset} for length {@code len}.
     */
    public boolean accept(char[] text, int offset, int len);

}