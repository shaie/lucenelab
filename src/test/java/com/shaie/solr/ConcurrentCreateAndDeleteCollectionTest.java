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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.shaie.solr.solrj.CollectionAdminHelper;
import com.shaie.solr.solrj.CreateCollectionResponse;
import com.shaie.solr.solrj.DeleteCollectionResponse;
import com.shaie.solr.utils.MiniSolrCloudClusterResource;
import com.shaie.utils.Utils;

public class ConcurrentCreateAndDeleteCollectionTest {

    private static final String CONFIG_NAME = ConcurrentCreateAndDeleteCollectionTest.class.getSimpleName();

    @Rule
    public final MiniSolrCloudClusterResource solrClusterResource = new MiniSolrCloudClusterResource(
            Utils.getFileResource("solr/solr.xml"));

    private final MiniSolrCloudCluster solrCluster = solrClusterResource.getSolrCluster();
    private final CloudSolrClient solrClient = new CloudSolrClient(solrClusterResource.getConnectString());
    private final CollectionAdminHelper collectionAdminHelper = new CollectionAdminHelper(solrClient);

    @Before
    public void setUp() {
        uploadConfigToZk(solrClient, CONFIG_NAME, Utils.getPathResource("solr/conf"));
        Logger.getLogger("org.apache.solr").setLevel(Level.WARN);
        solrCluster.startSolrNodes("node1");
    }

    @Test
    public void concurrent_delete_and_create_collection_over_same_config_works() {
        final AtomicBoolean failed = new AtomicBoolean(false);
        final Thread[] threads = new Thread[2];
        for (int i = 0; i < threads.length; i++) {
            final String collectionName = "collection" + i;
            threads[i] = new Thread("thread-" + i) {
                @Override
                public void run() {
                    final long timeToStop = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(60);
                    while (System.currentTimeMillis() < timeToStop && !failed.get()) {
                        createCollection();
                        deleteCollection();
                    }
                }

                @SuppressWarnings("synthetic-access")
                private void deleteCollection() {
                    final DeleteCollectionResponse deleteResponse = collectionAdminHelper
                            .deleteCollection(collectionName);
                    if (!deleteResponse.isSuccess()) {
                        failed.set(true);
                        throw new RuntimeException("Failed to delete collection " + collectionName);
                    }
                }

                @SuppressWarnings("synthetic-access")
                private void createCollection() {
                    final CreateCollectionResponse createResponse = collectionAdminHelper.createCollection(
                            collectionName, 1, 1, CONFIG_NAME);
                    if (!createResponse.isSuccess()) {
                        failed.set(true);
                        throw new RuntimeException("Failed to create collection " + collectionName);
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