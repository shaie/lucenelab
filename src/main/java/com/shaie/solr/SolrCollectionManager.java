package com.shaie.solr;

import java.io.IOException;
import java.util.List;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;

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

/** Manages collections of a Solr instance. */
public class SolrCollectionManager {

    @SuppressWarnings("unchecked")
    private static void listCollections(SolrClient solrClient) throws SolrServerException, IOException {
        final CollectionAdminRequest.List collectionListRequest = new CollectionAdminRequest.List();
        final CollectionAdminResponse response = collectionListRequest.process(solrClient);
        List<String> collections = (List<String>) response.getResponse().get("collections");
        System.out.println("Existing collections:");
        for (String c : collections) {
            System.out.println("  " + c);
        }
    }

    private static void createCollection(SolrClient solrClient, String collectionName) throws SolrServerException,
            IOException {
        final CollectionAdminRequest.Create createCollectionRequest = new CollectionAdminRequest.Create();
        createCollectionRequest.setCollectionName(collectionName);
        createCollectionRequest.setNumShards(1);
        createCollectionRequest.setConfigName("SIMPLE_CONFIG");
        System.out.println("Creating collection: " + collectionName);
        final CollectionAdminResponse response = createCollectionRequest.process(solrClient);
        System.out.println("  " + response);
    }

    private static void deleteCollection(SolrClient solrClient, String collectionName) throws SolrServerException,
            IOException {
        final CollectionAdminRequest.Delete deleteCollectionRequest = new CollectionAdminRequest.Delete();
        deleteCollectionRequest.setCollectionName(collectionName);
        System.out.println("Deleting collection: " + collectionName);
        final CollectionAdminResponse response = deleteCollectionRequest.process(solrClient);
        System.out.println("  " + response);
    }

    public static void main(String[] args) throws Exception {
        // final String zkHostString = "lnx-zeus.haifa.ibm.com:2181";
        // final SolrServer solr = new CloudSolrServer(zkHostString);
        final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        final SolrClient solr = new HttpSolrClient("http://localhost:7000/solr", httpClient);

        listCollections(solr);
        final String collectionName = "newCollection";
        createCollection(solr, collectionName);
        listCollections(solr);
        deleteCollection(solr, collectionName);

        solr.shutdown();
        httpClient.close();
    }

}