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

import static org.fest.assertions.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.DocCollection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.shaie.solr.solrj.CollectionAdminHelper;
import com.shaie.solr.utils.MiniSolrCloudClusterResource;
import com.shaie.utils.Utils;

public class SplitShardTest {

    private static final String REQUEST_ID = "my-async-id";
    private static final String CONFIG_NAME = "splitshard";
    private static final String COLLECTION_NAME = "mycollection";
    private static final long WAIT_TIMEOUT_SECONDS = 5;

    @Rule
    public final MiniSolrCloudClusterResource solrClusterResource = new MiniSolrCloudClusterResource(
            Utils.getFileResource("solr/solr.xml"));

    private final MiniSolrCloudCluster solrCluster = solrClusterResource.getSolrCluster();
    private final CloudSolrClient solrClient = new CloudSolrClient.Builder()
            .withZkHost(solrClusterResource.getConnectString())
            .build();
    private final CollectionAdminHelper collectionAdminHelper = new CollectionAdminHelper(solrClient);
    private final Random random = new Random();

    @Before
    public void setUp() {
        SolrCloudUtils.uploadConfigToZk(solrClient, CONFIG_NAME, Utils.getPathResource("solr/conf"));
        solrClient.setDefaultCollection(COLLECTION_NAME);
    }

    @Test
    public void split_shard_does_what_it_says() throws SolrServerException, IOException {
        System.out.println("ZK connection string: " + solrClusterResource.getConnectString());
        solrCluster.startSolrNodes("node1", "node2");
        createCollectionAndWaitForRecoveries();
        indexDocs(5000);

        final QueryResponse queryResponse = solrClient.query(new SolrQuery("*:*"));
        System.out.println("numResults=" + queryResponse.getResults().getNumFound());

        System.out.println("Press ENTER to print cluster status");
        System.in.read();
        printClusterStatus();

        System.out.println("Press ENTER to split shard1");
        System.in.read();
        splitShard();
        printClusterStatus();

        System.out.println("Press ENTER to start polling for request status");
        System.in.read();
        requestStatus();

        System.out.println("Press ENTER to print cluster status");
        System.in.read();
        printClusterStatus();

        System.out.println("Press ENTER to finish");
        System.in.read();
        // final boolean success = SolrCloudUtils.waitForNodeToDisappearFromLiveNodes(solrClient, node2Name,
        // WAIT_TIMEOUT_SECONDS);
        // assertThat(success).overridingErrorMessage("node " + node2Name + " didn't disappear from cluster's live
        // nodes")
        // .isTrue();
        //
        // final CollectionsStateHelper collectionsStateHelper = new
        // CollectionsStateHelper(solrClient.getZkStateReader());
        // final List<Replica> replicas = collectionsStateHelper.getAllCollectionReplicas(COLLECTION_NAME);
        // assertThat(replicas.size()).isEqualTo(2);
        // for (final Replica replica : replicas) {
        // assertThat(replica.getState()).isEqualTo(Replica.State.ACTIVE);
        // }
    }

    private void printClusterStatus() {
        final ClusterState clusterState = solrClient.getZkStateReader().getClusterState();
        System.out.println("live nodes: " + clusterState.getLiveNodes());
        for (final Entry<String, DocCollection> collection : clusterState.getCollectionsMap().entrySet()) {
            System.out.println(collection.getValue());
        }
    }

    private void createCollectionAndWaitForRecoveries() {
        collectionAdminHelper.createCollection(COLLECTION_NAME, 1, 2, CONFIG_NAME);
        SolrCloudUtils.waitForAllActive(COLLECTION_NAME, solrClient.getZkStateReader(), WAIT_TIMEOUT_SECONDS);
    }

    private static String generateRandomBody(Random random) {
        final int numWords = random.nextInt(900) + 100;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numWords; i++) {
            final int sentenceLength = random.nextInt(10) + 10;
            for (int j = 0; j < sentenceLength && i < numWords; j++) {
                final int wordLength = random.nextInt(7) + 3;
                sb.append(RandomStringUtils.randomAlphabetic(wordLength)).append(" ");
            }
            sb.setLength(sb.length() - 1);
            sb.append('.');
        }
        return sb.toString();
    }

    private void indexDocs(int numDocs) throws SolrServerException, IOException {
        final List<SolrInputDocument> docs = Lists.newArrayList();
        for (int i = 0; i < numDocs; i++) {
            final SolrInputDocument doc = new SolrInputDocument();
            doc.setField("id", "doc-" + i);
            doc.setField("body_t", generateRandomBody(random));
            docs.add(doc);

            if (i > 0 && (i % 100) == 0) {
                System.out.println("Indexed " + i + " docs.");
                final UpdateResponse addDocsResponse = solrClient.add(COLLECTION_NAME, docs);
                assertThat(addDocsResponse.getStatus()).isEqualTo(0);
                docs.clear();
            }
        }

        System.out.println("Indexed " + numDocs + " docs.");
        final UpdateResponse addDocsResponse = solrClient.add(COLLECTION_NAME, docs);
        assertThat(addDocsResponse.getStatus()).isEqualTo(0);

        System.out.println("Committing...");
        final UpdateResponse commitResponse = solrClient.commit(COLLECTION_NAME, true, true);
        assertThat(commitResponse.getStatus()).isEqualTo(0);
    }

    private void splitShard() throws SolrServerException, IOException {
        final CollectionAdminRequest.SplitShard splitShard = CollectionAdminRequest.splitShard(COLLECTION_NAME)
                .setShardName("shard1");
        splitShard.setAsyncId(REQUEST_ID);
        final CollectionAdminResponse response = splitShard.process(solrClient);
        System.out.println(response.getResponse());
    }

    private void requestStatus() throws SolrServerException, IOException {
        final CollectionAdminRequest.RequestStatus requestStatus = CollectionAdminRequest.requestStatus(REQUEST_ID);
        final CollectionAdminResponse response = requestStatus.process(solrClient);
        System.out.println(response.getResponse());
    }

}