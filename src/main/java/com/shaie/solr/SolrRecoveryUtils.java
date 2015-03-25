package com.shaie.solr;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.shaie.solr.solrj.CollectionAdminHelper;

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

/** Utilities for handling Solr node recoveries. */
public class SolrRecoveryUtils {

    private final CollectionsStateHelper collectionsStateHelper;
    private final CollectionAdminHelper collectionAdminHelper;

    public SolrRecoveryUtils(CollectionsStateHelper collectionsStateHelper, CollectionAdminHelper collectionAdminHelper) {
        this.collectionsStateHelper = collectionsStateHelper;
        this.collectionAdminHelper = collectionAdminHelper;
    }

    /** Finds a DOWN replica and takes over it by adding itself as a replica and deleting the DOWN one. */
    public void takeOverDownNode(String nodeName) {
        final Map<String, List<ReplicaInfo>> downNodes = collectionsStateHelper.getDownReplicas();
        if (!downNodes.isEmpty()) {
            final Entry<String, List<ReplicaInfo>> downNode = downNodes.entrySet().iterator().next();
            for (ReplicaInfo replicaInfo : downNode.getValue()) {
                final String collectionName = replicaInfo.getCollectionName();
                final String shardName = replicaInfo.getShardName();
                collectionAdminHelper.addReplica(collectionName, shardName, nodeName);
                collectionAdminHelper.deleteReplica(collectionName, shardName, replicaInfo.getReplica().getName());
            }
        }
    }

}