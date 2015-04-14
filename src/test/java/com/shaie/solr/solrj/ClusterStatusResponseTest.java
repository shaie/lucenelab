package com.shaie.solr.solrj;

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

import static com.shaie.solr.SolrCloudUtils.*;
import static org.fest.assertions.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.common.cloud.Slice;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.internal.Lists;
import com.shaie.solr.MiniSolrCloudCluster;
import com.shaie.solr.SolrCloudUtils;
import com.shaie.solr.solrj.ClusterStatusResponse.Collection;
import com.shaie.solr.utils.MiniSolrCloudClusterResource;
import com.shaie.utils.Utils;

public class ClusterStatusResponseTest {

    private static final String CONFIG_NAME = ClusterStatusResponseTest.class.getSimpleName();
    private static final long WAIT_TIMEOUT_SECONDS = 5;

    @Rule
    public final MiniSolrCloudClusterResource solrClusterResource = new MiniSolrCloudClusterResource(
            Utils.getFileResource("solr/solr.xml"));

    private final MiniSolrCloudCluster solrCluster = solrClusterResource.getSolrCluster();
    private final CloudSolrClient solrClient = new CloudSolrClient(solrClusterResource.getConnectString());
    private final CollectionAdminHelper collectionAdminHelper = new CollectionAdminHelper(solrClient);

    @Test
    public void cluster_status_parsed_successfully() throws SolrServerException, IOException {
        final String[] nodeIds = new String[] { "node1", "node2", "node3", "node4" };
        solrCluster.startSolrNodes("node1", "node2", "node3", "node4");

        initCluster();
        createAlias();
        addOverseerRole();

        final CollectionAdminRequest.ClusterStatus clusterStatusRequest = new CollectionAdminRequest.ClusterStatus();
        final CollectionAdminResponse response = clusterStatusRequest.process(solrClient);
        final ClusterStatusResponse clusterStatusResponse = new ClusterStatusResponse(response);

        assertResponseLiveNodes(nodeIds, clusterStatusResponse);
        assertThat(clusterStatusResponse.getAliases()).isEqualTo(ImmutableMap.of("both", "collection1,collection2"));
        assertThat(clusterStatusResponse.getRoles()).isEqualTo(
                ImmutableMap.of("overseer", Lists.newArrayList(
                        SolrCloudUtils.baseUrlToNodeName(solrCluster.getBaseUrl("node1")),
                        SolrCloudUtils.baseUrlToNodeName(solrCluster.getBaseUrl("node2")))));

        final Map<String, Collection> collections = clusterStatusResponse.getCollections();
        assertThat(collections.size()).isEqualTo(2);
        assertResponseCollection1(collections.get("collection1"));
        assertResponseCollection2(collections.get("collection2"));
    }

    private void initCluster() {
        uploadConfiguration();
        createCollectionAndWaitForRecoveries("collection1", 2, 2);
        createCollectionAndWaitForRecoveries("collection2", 3, 1);
    }

    private void assertResponseCollection1(final ClusterStatusResponse.Collection collection1) {
        assertThat(collection1.getName()).isEqualTo("collection1");
        assertThat(collection1.getAliases()).containsOnly("both");
        final List<Slice> collection1Slices = collection1.getSlices();
        assertThat(collection1Slices.size()).isEqualTo(2);
        for (Slice slice : collection1Slices) {
            assertThat(slice.getReplicas().size()).isEqualTo(2);
        }
    }

    private void assertResponseCollection2(ClusterStatusResponse.Collection collection2) {
        assertThat(collection2.getName()).isEqualTo("collection2");
        assertThat(collection2.getAliases()).containsOnly("both");
        final List<Slice> collection1Slices = collection2.getSlices();
        assertThat(collection1Slices.size()).isEqualTo(3);
        for (Slice slice : collection1Slices) {
            assertThat(slice.getReplicas().size()).isEqualTo(1);
        }
    }

    private void addOverseerRole() throws SolrServerException, IOException {
        final CollectionAdminRequest.AddRole addRoleRequest = new CollectionAdminRequest.AddRole();
        addRoleRequest.setRole("overseer");
        addRoleRequest.setNode(SolrCloudUtils.baseUrlToNodeName(solrCluster.getBaseUrl("node1")));
        addRoleRequest.process(solrClient);
        addRoleRequest.setNode(SolrCloudUtils.baseUrlToNodeName(solrCluster.getBaseUrl("node2")));
        addRoleRequest.process(solrClient);
    }

    private void createAlias() throws SolrServerException, IOException {
        final CollectionAdminRequest.CreateAlias createAliasRequest = new CollectionAdminRequest.CreateAlias();
        createAliasRequest.setAliasedCollections("collection1,collection2");
        createAliasRequest.setAliasName("both");
        createAliasRequest.process(solrClient);
    }

    private void assertResponseLiveNodes(final String[] nodeIds, final ClusterStatusResponse clusterStatusResponse) {
        final Object[] expectedNodeNames = new Object[nodeIds.length];
        for (int i = 0; i < expectedNodeNames.length; i++) {
            expectedNodeNames[i] = SolrCloudUtils.baseUrlToNodeName(solrCluster.getBaseUrl(nodeIds[i]));
        }
        assertThat(clusterStatusResponse.getLiveNodes()).containsOnly(expectedNodeNames);
    }

    private void uploadConfiguration() {
        uploadConfigToZk(solrClient, CONFIG_NAME, Utils.getPathResource("solr/conf"));
    }

    private void createCollectionAndWaitForRecoveries(String collectionName, int numShards, int numReplicas) {
        collectionAdminHelper.createCollection(collectionName, numShards, numReplicas, CONFIG_NAME);
        waitForAllActive(collectionName, solrClient.getZkStateReader(), WAIT_TIMEOUT_SECONDS);
    }

}