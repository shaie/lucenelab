package com.shaie.solr;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkStateReader;
import org.testng.collections.Maps;

import com.google.common.collect.Lists;

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

/** A helper for querying the state of Solr collections. */
public class CollectionsStateHelper {

    private final ZkStateReader zkStateReader;

    public CollectionsStateHelper(ZkStateReader zkStateReader) {
        this.zkStateReader = zkStateReader;
    }

    /** Returns the slices (shards) of a collection. */
    public Collection<Slice> getSlices(String collection) {
        return getClusterState().getSlices(collection);
    }

    /** Returns the active replicas of a collection. */
    public Collection<Replica> getActiveReplicas(String collection) {
        final List<Replica> activeReplicas = Lists.newArrayList();
        for (final Slice slice : getSlices(collection)) {
            if (!isSliceActive(slice)) {
                continue;
            }
            for (final Replica replica : slice.getReplicas()) {
                if (isReplicaActive(replica)) {
                    activeReplicas.add(replica);
                }
            }
        }
        return activeReplicas;
    }

    /** Returns the inactive replicas of a collection. */
    public Collection<Replica> getInactiveReplicas(String collection) {
        final List<Replica> inactiveReplicas = Lists.newArrayList();
        for (final Slice slice : getSlices(collection)) {
            if (!isSliceActive(slice)) {
                inactiveReplicas.addAll(slice.getReplicas());
            } else {
                for (final Replica replica : slice.getReplicas()) {
                    if (!isReplicaActive(replica)) {
                        inactiveReplicas.add(replica);
                    }
                }
            }
        }
        return inactiveReplicas;
    }

    /** Returns true if all the slices and replicas of a collection are active. */
    public boolean isCollectionFullyActive(String collection) {
        for (final Slice slice : getSlices(collection)) {
            if (!isSliceAndAllReplicasActive(slice)) {
                return false;
            }
        }
        return true;
    }

    /** Returns true if the slice and all its replicas are active. */
    public boolean isSliceAndAllReplicasActive(Slice slice) {
        if (!isSliceActive(slice)) {
            return false;
        }
        for (final Replica replica : slice.getReplicas()) {
            if (!isReplicaActive(replica)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true whether the slice is active. Note that the slice may be in ACTIVE state, but some of its replicas
     * may not.
     *
     * @see #isSliceAndAllReplicasActive(Slice)
     */
    public boolean isSliceActive(Slice slice) {
        return slice.getState() == Slice.State.ACTIVE;
    }

    /** Returns true if the replica is on a live node and active. */
    public boolean isReplicaActive(Replica replica) {
        return getClusterState().liveNodesContain(replica.getNodeName())
                && replica.getState() == Replica.State.ACTIVE;
    }

    /** Returns true if the replica is in a DOWN state. */
    public boolean isReplicaDown(Replica replica) {
        return !getClusterState().liveNodesContain(replica.getNodeName())
                || replica.getState() == Replica.State.DOWN;
    }

    /** Returns all the replicas (of all shards and collections) that exist on the given node. */
    public List<Replica> getAllNodeReplicas(String nodeName) {
        final List<Replica> replicas = Lists.newArrayList();
        final ClusterState clusterState = getClusterState();
        for (final String collection : clusterState.getCollections()) {
            for (final Slice slice : clusterState.getSlices(collection)) {
                for (final Replica replica : slice.getReplicas()) {
                    if (replica.getNodeName().equals(nodeName)) {
                        replicas.add(replica);
                    }
                }
            }
        }
        return replicas;
    }

    /** Returns all the replicas of all shards of the specified collection. */
    public List<Replica> getAllCollectionReplicas(String collection) {
        final List<Replica> replicas = Lists.newArrayList();
        for (final Slice slice : getClusterState().getSlices(collection)) {
            replicas.addAll(slice.getReplicas());
        }
        return replicas;
    }

    /** Returns true if the given node holds a replica of the given shard of the given collection. */
    public boolean isNodeReplicaOfShard(String collectionName, String shardName, String nodeName) {
        for (final Replica replica : getClusterState().getSlice(collectionName, shardName).getReplicas()) {
            if (replica.getNodeName().equals(nodeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all the nodes with down replicas. Note that some replicas for a node may not be marked DOWN, however per
     * node returned there is at least one replica that was marked DOWN.
     */
    public Map<String, List<ReplicaInfo>> getDownReplicas() {
        final Map<String, List<ReplicaInfo>> nodeReplicas = getNodeReplicas();
        final Map<String, List<ReplicaInfo>> result = Maps.newHashMap();
        for (final Entry<String, List<ReplicaInfo>> entry : nodeReplicas.entrySet()) {
            for (final ReplicaInfo replicaInfo : entry.getValue()) {
                if (isReplicaDown(replicaInfo.getReplica())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    /**
     * Obtains the {@link ClusterState} from the the {@link ZkStateReader}. This needs to be done periodically since the
     * state object reference changes as the cluster state changes.
     */
    public ClusterState getClusterState() {
        return zkStateReader.getClusterState();
    }

    /** Returns all replicas per node. */
    private Map<String, List<ReplicaInfo>> getNodeReplicas() {
        final ClusterState clusterState = getClusterState();
        final Map<String, List<ReplicaInfo>> result = Maps.newHashMap();
        for (final String collection : clusterState.getCollections()) {
            for (final Slice slice : clusterState.getSlices(collection)) {
                for (final Replica replica : slice.getReplicas()) {
                    List<ReplicaInfo> nodeReplicas = result.get(replica.getNodeName());
                    if (nodeReplicas == null) {
                        nodeReplicas = Lists.newArrayList();
                        result.put(replica.getNodeName(), nodeReplicas);
                    }
                    nodeReplicas.add(new ReplicaInfo(replica, collection, slice.getName()));
                }
            }
        }
        return result;
    }

}