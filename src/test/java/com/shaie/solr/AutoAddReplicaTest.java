package com.shaie.solr;

import static org.fest.assertions.Assertions.*;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.UpdateParams;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.shaie.solr.solrj.CollectionAdminHelper;
import com.shaie.solr.utils.MiniSolrCloudClusterResource;
import com.shaie.utils.Utils;

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

public class AutoAddReplicaTest {

    private static final String CONFIG_NAME = "autoAddReplicaTest";
    private static final String COLLECTION_NAME = "mycollection";
    private static final long WAIT_TIMEOUT_SECONDS = 5;

    @Rule
    public final MiniSolrCloudClusterResource solrClusterResource = new MiniSolrCloudClusterResource(
            Utils.getFileResource("solr/solr.xml"));

    private final MiniSolrCloudCluster solrCluster = solrClusterResource.getSolrCluster();
    private final CloudSolrClient solrClient = new CloudSolrClient(solrClusterResource.getConnectString());
    private final CollectionAdminHelper collectionAdminHelper = new CollectionAdminHelper(solrClient);

    @Before
    public void setUp() {
        SolrCloudUtils.uploadConfigToZk(solrClient, CONFIG_NAME, Utils.getPathResource("solr/conf"));
        solrClient.setDefaultCollection(COLLECTION_NAME);
    }

    @Test
    public void recovered_node_joins_as_replica() {
        solrCluster.startSolrNodes("node1", "node2");
        createCollectionAndWaitForRecoveries();
        indexDocumentAndWaitForSync("1");

        solrCluster.stopSolr("node2");
        sleepSome(500);
        solrCluster.startSolrNodes("node2");
        SolrCloudUtils.waitForAllActive(COLLECTION_NAME, solrClient.getZkStateReader(), WAIT_TIMEOUT_SECONDS);
        SolrCloudUtils.waitForReplicasToSync(COLLECTION_NAME, solrClient, WAIT_TIMEOUT_SECONDS);
        verifyReplicasState(2, 0);
    }

    @Test
    public void new_node_is_not_added_as_replica() {
        solrCluster.startSolrNodes("node1", "node2");
        createCollectionAndWaitForRecoveries();
        indexDocumentAndWaitForSync("1");

        solrCluster.stopSolr("node2");
        solrCluster.startSolrNodes("node3");
        SolrCloudUtils.waitForAllActive(COLLECTION_NAME, solrClient.getZkStateReader(), WAIT_TIMEOUT_SECONDS);
        SolrCloudUtils.waitForReplicasToSync(COLLECTION_NAME, solrClient, WAIT_TIMEOUT_SECONDS);
        verifyReplicasState(1, 1);
    }

    @Test
    public void new_node_can_take_over_down_replica() {
        solrCluster.startSolrNodes("node1", "node2");
        createCollectionAndWaitForRecoveries();
        indexDocumentAndWaitForSync("1");

        solrCluster.stopSolr("node2");
        sleepSome(200);
        solrCluster.startSolrNodes("node3");

        final SolrRecoveryUtils recoveryUtils = new SolrRecoveryUtils(new CollectionsStateHelper(
                solrClient.getZkStateReader()), collectionAdminHelper);
        final String nodeName = SolrCloudUtils.baseUrlToNodeName(solrCluster.getBaseUrl("node3"));
        recoveryUtils.takeOverDownNode(nodeName);
        SolrCloudUtils.waitForReplicasToSync(COLLECTION_NAME, solrClient, WAIT_TIMEOUT_SECONDS);

        verifyReplicasState(2, 0);
    }

    @Test
    public void new_node_can_take_over_all_replicas_of_a_down_node() {
        solrCluster.startSolrNodes("node1", "node2");
        createCollectionAndWaitForRecoveries();
        indexDocumentAndWaitForSync("1");

        final CollectionsStateHelper collectionsStateHelper = new CollectionsStateHelper(solrClient.getZkStateReader());
        final String node2Name = SolrCloudUtils.baseUrlToNodeName(solrCluster.getBaseUrl("node2"));
        collectionAdminHelper.addReplica(COLLECTION_NAME, "shard1", node2Name);
        SolrCloudUtils.waitForReplicasToSync(COLLECTION_NAME, solrClient, WAIT_TIMEOUT_SECONDS);

        assertThat(collectionsStateHelper.getAllNodeReplicas(node2Name).size()).isEqualTo(2);

        solrCluster.stopSolr("node2");
        sleepSome(200);
        solrCluster.startSolrNodes("node3");

        final SolrRecoveryUtils recoveryUtils = new SolrRecoveryUtils(new CollectionsStateHelper(
                solrClient.getZkStateReader()), collectionAdminHelper);
        final String node3Name = SolrCloudUtils.baseUrlToNodeName(solrCluster.getBaseUrl("node3"));
        recoveryUtils.takeOverDownNode(node3Name);
        SolrCloudUtils.waitForReplicasToSync(COLLECTION_NAME, solrClient, WAIT_TIMEOUT_SECONDS);
        assertThat(collectionsStateHelper.getAllNodeReplicas(node3Name).size()).isEqualTo(2);

        verifyReplicasState(3, 0);
    }

    @Test
    public void new_node_can_take_over_all_replicas_all_collections_of_a_down_node() {
        solrCluster.startSolrNodes("node1", "node2");
        final String[] collections = new String[] { "mycollection1", "mycollection2" };
        for (String collection : collections) {
            createCollectionAndWaitForRecoveries(collection);
            indexDocumentAndWaitForSync("1", collection);
        }

        final CollectionsStateHelper collectionsStateHelper = new CollectionsStateHelper(solrClient.getZkStateReader());
        final String node2Name = SolrCloudUtils.baseUrlToNodeName(solrCluster.getBaseUrl("node2"));
        assertThat(collectionsStateHelper.getAllNodeReplicas(node2Name).size()).isEqualTo(2);

        solrCluster.stopSolr("node2");
        sleepSome(200);
        solrCluster.startSolrNodes("node3");

        final SolrRecoveryUtils recoveryUtils = new SolrRecoveryUtils(new CollectionsStateHelper(
                solrClient.getZkStateReader()), collectionAdminHelper);
        final String node3Name = SolrCloudUtils.baseUrlToNodeName(solrCluster.getBaseUrl("node3"));
        recoveryUtils.takeOverDownNode(node3Name);
        for (String collection : collections) {
            SolrCloudUtils.waitForReplicasToSync(collection, solrClient, WAIT_TIMEOUT_SECONDS);
        }
        assertThat(collectionsStateHelper.getAllNodeReplicas(node3Name).size()).isEqualTo(2);

        for (String collection : collections) {
            verifyReplicasState(collection, 2, 0);
        }
    }

    private void createCollectionAndWaitForRecoveries() {
        createCollectionAndWaitForRecoveries(COLLECTION_NAME);
    }

    private void createCollectionAndWaitForRecoveries(String collectionName) {
        collectionAdminHelper.createCollection(collectionName, 1, 2, CONFIG_NAME);
        SolrCloudUtils.waitForAllActive(collectionName, solrClient.getZkStateReader(), WAIT_TIMEOUT_SECONDS);
    }

    private void indexDocumentAndWaitForSync(String docId) {
        indexDocumentAndWaitForSync(docId, COLLECTION_NAME);
    }

    private void indexDocumentAndWaitForSync(String docId, String collectionName) {
        final SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", docId);
        final UpdateRequest docUpdate = new UpdateRequest();
        docUpdate.add(doc);
        docUpdate.setParam(UpdateParams.COLLECTION, collectionName);
        docUpdate.setWaitSearcher(true);
        docUpdate.setParam(UpdateParams.SOFT_COMMIT, Boolean.TRUE.toString());
        try {
            final UpdateResponse response = docUpdate.process(solrClient);
            assertThat(response.getStatus()).isEqualTo(0);
            SolrCloudUtils.waitForReplicasToSync(collectionName, solrClient, WAIT_TIMEOUT_SECONDS);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sleepSome(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyReplicasState(int expectedNumActive, int expectedNumInactive) {
        verifyReplicasState(COLLECTION_NAME, expectedNumActive, expectedNumInactive);
    }

    private void verifyReplicasState(String collectionName, int expectedNumActive, int expectedNumInactive) {
        final CollectionsStateHelper collectionsStateHelper = new CollectionsStateHelper(solrClient.getZkStateReader());
        assertThat(collectionsStateHelper.getActiveReplicas(collectionName).size()).isEqualTo(expectedNumActive);
        assertThat(collectionsStateHelper.getInactiveReplicas(collectionName).size()).isEqualTo(expectedNumInactive);
    }

}