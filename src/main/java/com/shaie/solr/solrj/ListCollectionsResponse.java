package com.shaie.solr.solrj;

import java.util.List;

import org.apache.solr.common.util.NamedList;

import com.google.common.collect.ImmutableList;

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

/** A {@link SolrResponse} for {@link org.apache.solr.client.solrj.request.CollectionAdminRequest.List} requests. */
public class ListCollectionsResponse extends CollectionAdminResponse {

    public static ListCollectionsResponse from(
            org.apache.solr.client.solrj.response.CollectionAdminResponse collectionAdminResponse) {
        return new ListCollectionsResponse(collectionAdminResponse);
    }

    private final List<String> collections;

    @SuppressWarnings("unchecked")
    private ListCollectionsResponse(
            org.apache.solr.client.solrj.response.CollectionAdminResponse listCollectionsResponse) {
        super(listCollectionsResponse);

        final NamedList<Object> response = rawResponse.getResponse();
        this.collections = ImmutableList.copyOf((List<String>) response.get("collections"));
    }

    /** Returns the list of collections returned from the request. */
    public List<String> getCollections() {
        return collections;
    }

    /** Returns true if {@code collectionName} was returned in the response. */
    public boolean hasCollection(String collectionName) {
        return collections.contains(collectionName);
    }

}