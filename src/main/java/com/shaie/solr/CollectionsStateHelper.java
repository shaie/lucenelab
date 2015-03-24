package com.shaie.solr;

import java.util.Collection;

import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;

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

    private final ClusterState clusterState;

    public CollectionsStateHelper(ClusterState clusterState) {
        this.clusterState = clusterState;
    }

    /** Returns the slices (shards) of a collection. */
    public Collection<Slice> getSlices(String collection) {
        return clusterState.getSlices(collection);
    }

    /** Returns the active slices of a collection. */
    public Collection<Slice> getActiveSlices(String collection) {
        return clusterState.getActiveSlices(collection);
    }

    /** Returns true if all the slices and replicas of a collection are active. */
    public boolean isCollectionFullyActive(String collection) {
        for (Slice slice : getSlices(collection)) {
            if (!areSliceAndReplicasActive(slice)) {
                return false;
            }
        }
        return true;
    }

    /** Returns true if the slice and all its replicas are active. */
    public boolean areSliceAndReplicasActive(Slice slice) {
        if (!slice.getState().equals(Slice.ACTIVE)) {
            return false;
        }
        for (Replica replica : slice.getReplicas()) {
            if (!isReplicaActive(replica)) {
                return false;
            }
        }
        return true;
    }

    /** Returns true if the replica is on a live node and active. */
    public boolean isReplicaActive(Replica replica) {
        return clusterState.liveNodesContain(replica.getNodeName()) && replica.getStr(Slice.STATE).equals(Slice.ACTIVE);
    }

}