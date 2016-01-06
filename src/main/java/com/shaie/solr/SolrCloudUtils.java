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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.shaie.utils.Waiter;

public class SolrCloudUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrCloudUtils.class);

    private static final long DEFAULT_POLL_INTERVAL_MS = 500;

    public static final String ZK_HOST_PROP_NAME = "zkHost";

    private SolrCloudUtils() {
        // should not be instantiated
    }

    /** Uploads configuration files to ZooKeeper. */
    public static void uploadConfigToZk(CloudSolrClient solrClient, String configName, Path confDir) {
        try {
            solrClient.uploadConfig(confDir, configName);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns the collection names that were created with the given configuration name. */
    @SuppressWarnings("resource")
    public static List<String> getCollectionsCreatedWithConfig(CloudSolrClient solrClient, String configName) {
        final List<String> result = Lists.newArrayList();
        final ZkStateReader zkStateReader = solrClient.getZkStateReader();
        for (final String collection : zkStateReader.getClusterState().getCollections()) {
            final String collectionConfigName = getCollectionConfigName(zkStateReader, collection);
            if (configName.equals(collectionConfigName)) {
                result.add(collection);
            }
        }
        return result;
    }

    /** Returns a collection's configuration name, or {@code null} if the collection doesn't exist. */
    public static String getCollectionConfigName(ZkStateReader zkStateReader, String collection) {
        try {
            return zkStateReader.readConfigName(collection);
        } catch (final SolrException e) {
            if (e.getCause() instanceof NoNodeException) {
                return null;
            }
            throw e;
        }
    }

    /** Waits until all replicas of all slices of the collection are active, or the timeout has expired. */
    public static boolean waitForAllActive(final String collection, ZkStateReader zkStateReader, long timeoutSeconds) {
        final CollectionsStateHelper collectionsStateHelper = new CollectionsStateHelper(zkStateReader);
        return Waiter.waitFor(new Waiter.Condition() {
            @SuppressWarnings("synthetic-access")
            @Override
            public boolean isSatisfied() {
                final boolean result = collectionsStateHelper.isCollectionFullyActive(collection);
                if (!result) {
                    LOGGER.info("Not all replicas of collection [{}] are active:\n"
                            + "active_replicas=[{}],\n"
                            + "inactive_replicas=[{}]",
                            collection, collectionsStateHelper.getActiveReplicas(collection),
                            collectionsStateHelper.getInactiveReplicas(collection));
                }
                return result;
            }
        }, timeoutSeconds, TimeUnit.SECONDS, 500, TimeUnit.MILLISECONDS);
    }

    /** Waits until all replicas of the collection are in sync. */
    public static boolean waitForReplicasToSync(final String collection, CloudSolrClient solrClient,
            long timeoutSeconds) {
        final ReplicasSyncVerifier verifier = new ReplicasSyncVerifier(solrClient);
        return Waiter.waitFor(new Waiter.Condition() {
            @Override
            public boolean isSatisfied() {
                return verifier.verify(collection);
            }
        }, timeoutSeconds, TimeUnit.SECONDS, DEFAULT_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /** Waits for the given node to disappear from the cluster's live nodes. */
    public static boolean waitForNodeToDisappearFromLiveNodes(final CloudSolrClient solrClient, final String nodeName,
            long timeoutSeconds) {
        return Waiter.waitFor(new Waiter.Condition() {
            @Override
            public boolean isSatisfied() {
                return !solrClient.getZkStateReader().getClusterState().liveNodesContain(nodeName);
            }
        }, timeoutSeconds, TimeUnit.SECONDS, DEFAULT_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /** Returns a Solr node's base URL to a node name as appears */
    public static String baseUrlToNodeName(String baseUrl) {
        final URI baseUri = URI.create(baseUrl);
        final StringBuilder sb = new StringBuilder();
        sb.append(baseUri.getHost()).append(':').append(baseUri.getPort()).append('_');
        if (baseUri.getPath().startsWith("/")) {
            sb.append(baseUri.getPath().substring(1));
        } else {
            sb.append(baseUri.getPath());
        }
        return sb.toString();
    }

}