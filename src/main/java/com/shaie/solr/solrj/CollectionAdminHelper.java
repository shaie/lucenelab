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

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;

/** A helper class for interacting with Solr collections. */
public class CollectionAdminHelper {

    private final SolrClient solrClient;

    public CollectionAdminHelper(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    /** Returns true if the collection denoted by {@code collectionName} exists. */
    public boolean collectionExists(String collectionName) {
        try {
            final CollectionAdminRequest.List listRequest = new CollectionAdminRequest.List();
            final CollectionAdminResponse listResponse = listRequest.process(solrClient);
            final ListCollectionsResponse listCollectionsResponse = ListCollectionsResponse.from(listResponse);
            return listCollectionsResponse.hasCollection(collectionName);
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    /** Creates a collection. */
    public CreateCollectionResponse createCollection(String collectionName, int numShards, int numReplicas,
            String configName) {
        if (collectionExists(collectionName)) {
            return null;
        }

        try {
            final CollectionAdminRequest.Create createCollectionRequest = new CollectionAdminRequest.Create();
            createCollectionRequest.setCollectionName(collectionName);
            createCollectionRequest.setNumShards(numShards);
            createCollectionRequest.setReplicationFactor(numReplicas);
            createCollectionRequest.setConfigName(configName);

            final CollectionAdminResponse response = createCollectionRequest.process(solrClient);
            return new CreateCollectionResponse(response);
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    /** Deletes a collection. */
    public DeleteCollectionResponse deleteCollection(String collectionName) {
        if (!collectionExists(collectionName)) {
            return null;
        }

        try {
            final CollectionAdminRequest.Delete deleteCollectionRequest = new CollectionAdminRequest.Delete();
            deleteCollectionRequest.setCollectionName(collectionName);

            final CollectionAdminResponse response = deleteCollectionRequest.process(solrClient);
            return new DeleteCollectionResponse(response);
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    /** Adds a replica to the given collection and shard. */
    public AddReplicaResponse addReplica(String collectionName, String shardName, String nodeName) {
        if (!collectionExists(collectionName)) {
            throw new IllegalArgumentException("collection [" + collectionName + "] does not exist");
        }

        try {
            final CollectionAdminRequest.AddReplica addReplicaRequest = new CollectionAdminRequest.AddReplica();
            addReplicaRequest.setCollectionName(collectionName);
            addReplicaRequest.setShardName(shardName);
            addReplicaRequest.setNode(nodeName);
            final CollectionAdminResponse response = addReplicaRequest.process(solrClient);
            return new AddReplicaResponse(response);
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes a replica from the given collection and shard. If the replica's node does not respond, the replica will
     * be deleted only from ZK.
     */
    public void deleteReplica(String collectionName, String shardName, String replicaName) {
        if (!collectionExists(collectionName)) {
            throw new IllegalArgumentException("collection [" + collectionName + "] does not exist");
        }

        try {
            final CollectionAdminRequest.DeleteReplica deleteReplicaRequest =
                    new CollectionAdminRequest.DeleteReplica();
            deleteReplicaRequest.setCollectionName(collectionName);
            deleteReplicaRequest.setShardName(shardName);
            deleteReplicaRequest.setReplica(replicaName);
            deleteReplicaRequest.setOnlyIfDown(false);
            deleteReplicaRequest.process(solrClient);
        } catch (IOException | SolrServerException e) {
            System.err.println("here");
            throw new RuntimeException(e);
        }
    }

}