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
package com.shaie.solr.solrj;

import org.apache.solr.client.solrj.response.SolrResponseBase;

/** Base class for all Solr responses. */
public abstract class SolrResponse<T extends SolrResponseBase> {

    protected final T rawResponse;

    protected SolrResponse(T rawResponse) {
        this.rawResponse = rawResponse;
    }

    /**
     * Returns the time took to execute the request, including the "server side" time. That is, if the request is
     * executed over HTTP, this time contains the HTTP transport time as well as any pre and post processing that
     * happened on the client side.
     */
    public long getElapsedTime() {
        return rawResponse.getElapsedTime();
    }

    /** Returns the time it took to execute the request by the server. */
    public int getQTime() {
        return rawResponse.getQTime();
    }

    /** Returns the status of the request. Any value other than {@code 0} is considered an error. */
    public int getStatus() {
        return rawResponse.getStatus();
    }

    /** Returns the URL that the request was translated into. */
    public String getRequestUrl() {
        return rawResponse.getRequestUrl();
    }

    /** Returns the raw {@link org.apache.solr.client.solrj.SolrResponse}. */
    public T getRawResponse() {
        return rawResponse;
    }

}