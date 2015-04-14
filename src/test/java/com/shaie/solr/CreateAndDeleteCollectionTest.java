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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import com.carrotsearch.ant.tasks.junit4.dependencies.com.google.common.base.Throwables;
import com.shaie.solr.solrj.CollectionAdminHelper;
import com.shaie.solr.solrj.DeleteCollectionResponse;
import com.shaie.solr.utils.MiniSolrCloudClusterResource;
import com.shaie.utils.Utils;

public class CreateAndDeleteCollectionTest {

    @Rule
    public final MiniSolrCloudClusterResource solrClusterResource = new MiniSolrCloudClusterResource(
            Utils.getFileResource("solr/solr.xml"));

    private final MiniSolrCloudCluster solrCluster = solrClusterResource.getSolrCluster();
    private final CloudSolrClient solrClient = new CloudSolrClient(solrClusterResource.getConnectString());
    private final CollectionAdminHelper collectionAdminHelper = new CollectionAdminHelper(solrClient);

    @After
    public void tearDown() throws IOException {
        solrClient.close();
    }

    @Test
    public void create_and_delete_collection_concurrently_works() {
        solrCluster.startSolrNodes("node1");

        final AtomicBoolean failed = new AtomicBoolean(false);
        final int timeToRunSec = 30;
        final Thread[] threads = new Thread[4];
        for (int i = 0; i < threads.length; i++) {
            final String collectionName = "collection" + i;
            threads[i] = new Thread("thread-" + i) {
                @SuppressWarnings("synthetic-access")
                @Override
                public void run() {
                    synchronized (solrClient) {
                        SolrCloudUtils.uploadConfigToZk(solrClient, collectionName, Utils.getPathResource("solr/conf"));
                    }
                    final long timeToStop = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeToRunSec);
                    while (System.currentTimeMillis() < timeToStop && !failed.get()) {
                        createCollection(collectionName);
                        deleteCollection();
                        assertCollectionDoesNotExist();
                    }
                }

                @SuppressWarnings("synthetic-access")
                private void assertCollectionDoesNotExist() {
                    try (final HttpSolrClient httpSolrClient = new HttpSolrClient(solrCluster.getBaseUrl("node1"))) {
                        final QueryResponse response = httpSolrClient.query(collectionName, new SolrQuery("*"));
                        if (response.getStatus() != 0) {
                            failed.set(true);
                        }
                    } catch (Exception e) {
                        if (!e.getMessage().contains("not found") && !e.getMessage().contains("Can not find")) {
                            Throwables.propagate(e);
                        }
                    }
                }

                @SuppressWarnings("synthetic-access")
                private void createCollection(String collectionName) {
                    collectionAdminHelper.createCollection(collectionName, 1, 1, collectionName);
                }

                @SuppressWarnings("synthetic-access")
                private void deleteCollection() {
                    final DeleteCollectionResponse response = collectionAdminHelper.deleteCollection(collectionName);
                    if (response != null) {
                        assertThat(response.isSuccess()).isTrue();
                    }
                }
            };
        }

        startAll(threads);
        joinAll(threads);

        assertThat(failed.get()).isFalse();
    }

    private void joinAll(final Thread[] threads) {
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.interrupted();
                throw new RuntimeException(e);
            }
        }
    }

    private void startAll(final Thread[] threads) {
        for (Thread t : threads) {
            t.start();
        }
    }

}