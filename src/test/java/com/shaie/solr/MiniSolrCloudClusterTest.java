package com.shaie.solr;

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

import static org.fest.assertions.Assertions.*;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.params.UpdateParams;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.shaie.solr.solrj.CollectionAdminHelper;
import com.shaie.solr.utils.MiniSolrCloudClusterResource;
import com.shaie.utils.Utils;

public class MiniSolrCloudClusterTest {

    private static final String CONFIG_NAME = "miniSolrCloudTest";
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
    public void kill_node_does_not_gracefully_shutdown() {
        solrCluster.startSolrNodes("node1", "node2");
        createCollectionAndWaitForRecoveries();
        indexDocumentAndWaitForSync("1");

        final String node2Name = SolrCloudUtils.baseUrlToNodeName(solrCluster.getBaseUrl("node2"));
        solrCluster.killSolr("node2");

        final boolean success = SolrCloudUtils.waitForNodeToDisappearFromLiveNodes(solrClient, node2Name,
                WAIT_TIMEOUT_SECONDS);
        assertThat(success).overridingErrorMessage("node " + node2Name + " didn't disappear from cluster's live nodes")
                .isTrue();

        final CollectionsStateHelper collectionsStateHelper = new CollectionsStateHelper(solrClient.getZkStateReader());
        final List<Replica> replicas = collectionsStateHelper.getAllCollectionReplicas(COLLECTION_NAME);
        assertThat(replicas.size()).isEqualTo(2);
        for (final Replica replica : replicas) {
            assertThat(replica.getState()).isEqualTo(Replica.State.ACTIVE);
        }
    }

    private void createCollectionAndWaitForRecoveries() {
        collectionAdminHelper.createCollection(COLLECTION_NAME, 1, 2, CONFIG_NAME);
        SolrCloudUtils.waitForAllActive(COLLECTION_NAME, solrClient.getZkStateReader(), WAIT_TIMEOUT_SECONDS);
    }

    private void indexDocumentAndWaitForSync(String docId) {
        final SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", docId);
        final UpdateRequest docUpdate = new UpdateRequest();
        docUpdate.add(doc);
        docUpdate.setParam(UpdateParams.COLLECTION, COLLECTION_NAME);
        docUpdate.setWaitSearcher(true);
        docUpdate.setParam(UpdateParams.SOFT_COMMIT, Boolean.TRUE.toString());
        try {
            final UpdateResponse response = docUpdate.process(solrClient);
            assertThat(response.getStatus()).isEqualTo(0);
            SolrCloudUtils.waitForReplicasToSync(COLLECTION_NAME, solrClient, WAIT_TIMEOUT_SECONDS);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}