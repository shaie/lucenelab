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

import static com.shaie.solr.SolrCloudUtils.*;
import static org.fest.assertions.Assertions.*;

import java.io.IOException;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.UpdateParams;
import org.junit.Rule;
import org.junit.Test;

import com.shaie.solr.solrj.CollectionAdminHelper;
import com.shaie.solr.utils.MiniSolrCloudClusterResource;
import com.shaie.utils.Utils;

public class ConfigurationManagementTest {

    private static final String CONFIG_NAME = ConfigurationManagementTest.class.getSimpleName();
    private static final String COLLECTION_NAME = "mycollection";
    private static final long WAIT_TIMEOUT_SECONDS = 5;

    @Rule
    public final MiniSolrCloudClusterResource solrClusterResource = new MiniSolrCloudClusterResource(
            Utils.getFileResource("solr/solr.xml"));

    private final MiniSolrCloudCluster solrCluster = solrClusterResource.getSolrCluster();
    private final CloudSolrClient solrClient = new CloudSolrClient(solrClusterResource.getConnectString());
    private final CollectionAdminHelper collectionAdminHelper = new CollectionAdminHelper(solrClient);

    @Test
    public void delete_configuration_when_collection_still_exists() {
        solrCluster.startSolrNodes("node1");
        uploadConfiguration();
        createCollectionAndWaitForRecoveries();
        indexDocumentAndWaitForSync("doc1");
        assertCoreHasNumSearchableDocs(1);
        assertThat(getCollectionsCreatedWithConfig(solrClient, CONFIG_NAME)).containsExactly(COLLECTION_NAME);
        assertThat(getCollectionConfigName(solrClient.getZkStateReader(), COLLECTION_NAME)).isEqualTo(CONFIG_NAME);

        // this screws up the collection
        System.out.println("+++ delete");
        deleteConfiguration();

        // final CollectionsStateHelper collectionsStateHelper = new
        // CollectionsStateHelper(solrClient.getZkStateReader());
        // final Replica replica = collectionsStateHelper.getAllCollectionReplicas(COLLECTION_NAME).get(0);
        // final String coreName = replica.getStr(ZkStateReader.CORE_NAME_PROP);
        // assertCoreHasInitFailures(coreName);
        //
        // re-upload configuration
        System.out.println("+++ re-upload");
        uploadConfiguration();

        // try {
        // Thread.sleep(100);
        // } catch (InterruptedException e1) {
        // // TODO Auto-generated catch block
        // e1.printStackTrace();
        // }
        // try {
        // CoreAdminRequest.reloadCore(coreName, solrClient);
        // } catch (SolrServerException | IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // assertCoreHasInitFailures(coreName);

        // try index another document
        System.out.println("+++ index doc2");
        indexDocumentAndWaitForSync("doc2");
        assertCoreHasNumSearchableDocs(2);
        //
        // try {
        // System.out.println("+++ " + CoreAdminRequest.getStatus(coreName, solrClient));
        // } catch (SolrServerException | IOException e) {
        // throw new RuntimeException(e);
        // }

        System.out.println("+++ restart node1");
        solrCluster.stopSolr("node1");
        solrCluster.startSolrNodes("node1");
        SolrCloudUtils.waitForReplicasToSync(COLLECTION_NAME, solrClient, WAIT_TIMEOUT_SECONDS);

        System.out.println("+++ index doc3");
        indexDocumentAndWaitForSync("doc3");
        assertCoreHasNumSearchableDocs(3);

        System.out.println("+++ done");
    }

    private void assertCoreHasInitFailures(final String coreName) {
        final Map<String, Object> initFailures = getCoreInitFailures(coreName);
        assertThat((String) initFailures.get(coreName))
                .isNotNull()
                .contains("Specified config does not exist in ZooKeeper")
                .contains(CONFIG_NAME);
    }

    private void uploadConfiguration() {
        uploadConfigToZk(solrClusterResource.getConnectString(), CONFIG_NAME, Utils.getFileResource("solr/conf"));
    }

    private void deleteConfiguration() {
        deleteConfigFromZk(solrClusterResource.getConnectString(), CONFIG_NAME);
    }

    private void createCollectionAndWaitForRecoveries() {
        collectionAdminHelper.createCollection(COLLECTION_NAME, 1, 1, CONFIG_NAME);
        waitForAllActive(COLLECTION_NAME, solrClient.getZkStateReader(), WAIT_TIMEOUT_SECONDS);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCoreInitFailures(String coreName) {
        try {
            final CoreAdminResponse response = CoreAdminRequest.getStatus(coreName, solrClient);
            return (Map<String, Object>) response.getResponse().get("initFailures");
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
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

    private void assertCoreHasNumSearchableDocs(int expectedNumDocs) {
        final SolrQuery query = new SolrQuery("*:*");
        query.setParam(UpdateParams.COLLECTION, COLLECTION_NAME);
        try {
            final QueryResponse response = solrClient.query(query);
            assertThat(response.getResults().getNumFound()).isEqualTo(expectedNumDocs);
        } catch (SolrServerException e) {
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

}