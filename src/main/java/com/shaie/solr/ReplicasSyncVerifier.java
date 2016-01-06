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
package com.shaie.solr;

import org.apache.solr.client.solrj.impl.CloudSolrClient;

/** Verifies that all of a collection's replicas are in sync. */
public class ReplicasSyncVerifier {

    private final CloudSolrClient solrClient;

    public ReplicasSyncVerifier(CloudSolrClient solrClient) {
        this.solrClient = solrClient;
    }

    /**
     * Verifies that all replicas of the collection are in sync. Replicas are considered "sync'd" if they are active.
     * Otherwise it means they are either down or recovering, in which case we cannot trust that they are fully sync'd.
     */
    public boolean verify(String collection) {
        final CollectionsStateHelper collectionsStateHelper = new CollectionsStateHelper(solrClient.getZkStateReader());
        return collectionsStateHelper.isCollectionFullyActive(collection);
    }
}