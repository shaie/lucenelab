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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.util.NamedList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ClusterStatusResponse extends CollectionAdminResponse {

    private final Map<String, Collection> collections;
    private final Map<String, String> aliases;
    private final Map<String, List<String>> roles;
    private final List<String> liveNodes;

    @SuppressWarnings("unchecked")
    public ClusterStatusResponse(org.apache.solr.client.solrj.response.CollectionAdminResponse rawResponse) {
        super(rawResponse);

        if (rawResponse.getStatus() == 0) {
            final NamedList<Object> cluster = (NamedList<Object>) rawResponse.getResponse().get("cluster");
            collections = getCollections(cluster);
            aliases = (Map<String, String>) cluster.get("aliases");
            roles = (Map<String, List<String>>) cluster.get("roles");
            liveNodes = (List<String>) cluster.get("live_nodes");
        } else {
            collections = null;
            aliases = null;
            roles = null;
            liveNodes = null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Collection> getCollections(NamedList<Object> cluster) {
        final Map<String, Collection> collections = Maps.newHashMap();
        for (final Entry<String, Object> collectionEntry : (NamedList<Object>) cluster.get("collections")) {
            final String name = collectionEntry.getKey();
            final Map<String, Object> collectionValue = (Map<String, Object>) collectionEntry.getValue();
            final int maxShardsPerNode = Integer.parseInt((String) collectionValue.get("maxShardsPerNode"));
            final int replicationFactor = Integer.parseInt((String) collectionValue.get("replicationFactor"));
            final boolean autoCreated = Boolean.parseBoolean((String) collectionValue.get("autoCreated"));
            final List<String> aliases = (List<String>) collectionValue.get("aliases");
            final List<Slice> slices = getSlices(collectionValue);
            collections.put(name, new Collection(name, maxShardsPerNode, replicationFactor, autoCreated, aliases,
                    slices));
        }
        return collections;
    }

    @SuppressWarnings("unchecked")
    private List<Slice> getSlices(final Map<String, Object> collectionValue) {
        final List<Slice> slices = Lists.newArrayList();
        for (final Entry<String, Object> shardEntry : ((Map<String, Object>) collectionValue.get("shards"))
                .entrySet()) {
            final Map<String, Object> shardValue = (Map<String, Object>) shardEntry.getValue();
            final Map<String, Replica> shardReplicas = getSliceReplicas(shardValue);
            slices.add(new Slice(shardEntry.getKey(), shardReplicas, shardValue));
        }
        return slices;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Replica> getSliceReplicas(final Map<String, Object> shardValue) {
        final Map<String, Replica> shardReplicas = Maps.newHashMap();
        for (final Entry<String, Object> replicaEntry : ((Map<String, Object>) shardValue.get("replicas")).entrySet()) {
            final String replicaName = replicaEntry.getKey();
            final Replica replica = new Replica(replicaName, (Map<String, Object>) replicaEntry.getValue());
            shardReplicas.put(replicaName, replica);
        }
        return shardReplicas;
    }

    public Map<String, String> getAliases() {
        return aliases;
    }

    public Map<String, Collection> getCollections() {
        return collections;
    }

    public List<String> getLiveNodes() {
        return liveNodes;
    }

    public Map<String, List<String>> getRoles() {
        return roles;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("collections: ").append(collections).append('\n');
        sb.append("aliases: ").append(aliases).append('\n');
        sb.append("roles: ").append(roles).append('\n');
        sb.append("liveNodes: ").append(liveNodes).append('\n');
        return sb.toString();
    }

    public static class Collection {

        private final String name;
        private final int maxShardsPerNode;
        private final int replicationFactor;
        private final boolean autoCreated;
        private final List<String> aliases;
        private final List<Slice> slices;

        public Collection(String name, int maxShardsPerNode, int replicationFactor, boolean autoCreated,
                List<String> aliases, List<Slice> slices) {
            this.name = name;
            this.maxShardsPerNode = maxShardsPerNode;
            this.replicationFactor = replicationFactor;
            this.autoCreated = autoCreated;
            this.aliases = aliases;
            this.slices = slices;
        }

        public String getName() {
            return name;
        }

        public int getMaxShardsPerNode() {
            return maxShardsPerNode;
        }

        public int getReplicationFactor() {
            return replicationFactor;
        }

        public boolean isAutoCreated() {
            return autoCreated;
        }

        public List<String> getAliases() {
            return aliases;
        }

        public List<Slice> getSlices() {
            return slices;
        }

        @Override
        public String toString() {
            try {
                return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
            } catch (final JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

}