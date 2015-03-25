package com.shaie.solr;

import java.io.File;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkStateReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.ant.tasks.junit4.dependencies.com.google.common.base.Throwables;
import com.shaie.utils.Waiter;

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

public class SolrCloudUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrCloudUtils.class);

    public static final String SOLRXML_LOCATION_PROP_NAME = "solr.solrxml.location";
    public static final String SOLRXML_LOCATION_PROP_VALUE = "zookeeper";
    public static final String ZK_HOST_PROP_NAME = "zkHost";

    private SolrCloudUtils() {
        // should not be instantiated
    }

    /** Uploads configuration files and solr.xml to ZooKeeper. */
    public static void uploadConfigToZk(String connectString, String configName, File confDir, File solrXml) {
        try (final SolrZkClient zkClient = new SolrZkClient(connectString, 120000)) {
            ZkController.uploadConfigDir(zkClient, confDir, configName);
            zkClient.makePath("/solr.xml", solrXml, false, true);
            System.setProperty(SOLRXML_LOCATION_PROP_NAME, SOLRXML_LOCATION_PROP_VALUE);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    /** Waits until all replicas of all slices of the collection are active, or the timeout has expired. */
    public static void waitForAllActive(final String collection, ZkStateReader zkStateReader, long timeoutSeconds) {
        final CollectionsStateHelper collectionsStateHelper = new CollectionsStateHelper(zkStateReader);
        Waiter.waitFor(new Waiter.Condition() {
            @SuppressWarnings("synthetic-access")
            @Override
            public boolean isSatisfied() {
                boolean result = collectionsStateHelper.isCollectionFullyActive(collection);
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
    public static void waitForReplicasToSync(final String collection, CloudSolrClient solrClient, long timeoutSeconds) {
        final ReplicasSyncVerifier verifier = new ReplicasSyncVerifier(solrClient);
        Waiter.waitFor(new Waiter.Condition() {
            @Override
            public boolean isSatisfied() {
                return verifier.verify(collection);
            }
        }, timeoutSeconds, TimeUnit.SECONDS, 500, TimeUnit.MILLISECONDS);
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