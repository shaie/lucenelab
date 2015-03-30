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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.SolrZooKeeper;
import org.apache.solr.servlet.SolrDispatchFilter;

import com.carrotsearch.ant.tasks.junit4.dependencies.com.google.common.base.Throwables;
import com.google.common.collect.Sets;

/** Simulates a SolrCloud cluster by creating allowing to start/stop nodes. */
public class MiniSolrCloudCluster implements AutoCloseable {

    public static final String SOLRXML_LOCATION_PROP_NAME = "solr.solrxml.location";
    public static final String SOLRXML_LOCATION_PROP_VALUE = "zookeeper";

    private static final String SOLR_CONTEXT = "/solr";

    private final File workDir;
    private final Map<String, JettySolrRunner> solrRunners = new HashMap<>();

    public MiniSolrCloudCluster(File workDir, File solrXml, String connectString) {
        this.workDir = workDir;
        try (final SolrZkClient zkClient = new SolrZkClient(connectString, 120000)) {
            zkClient.makePath("/solr.xml", solrXml, false, true);
            System.setProperty(SOLRXML_LOCATION_PROP_NAME, SOLRXML_LOCATION_PROP_VALUE);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    /** Starts multiple Solr nodes. */
    public void startSolrNodes(String... nodeIDs) {
        for (String nodeId : nodeIDs) {
            startSolrNode(nodeId);
        }
    }

    /** Stops the Solr identified by the given {@code nodeId}. */
    public void stopSolr(String nodeId) {
        final JettySolrRunner solrRunner = getJettySolrRunner(nodeId);
        try {
            solrRunner.stop();
            solrRunners.remove(nodeId);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Kills the Solr instance identified by the given {@code nodeId}. Unlike {@link #stopSolr(String)}, this method
     * prevents Solr from doing a graceful shutdown, so that states recorded in ZooKeeper aren't consistent.
     */
    public void killSolr(String nodeId) {
        final JettySolrRunner solrRunner = getJettySolrRunner(nodeId);
        final SolrDispatchFilter solrFilter = (SolrDispatchFilter) solrRunner.getDispatchFilter().getFilter();
        final SolrZooKeeper solrZooKeeper = solrFilter.getCores().getZkController().getZkClient().getSolrZooKeeper();
        try {
            // solrZooKeeper.closeCnxn() doesn't really work
            solrZooKeeper.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        stopSolr(nodeId);
    }

    /**
     * Destroys the Solr instance identified by the given {@code nodeId}. Beyond just stopping it, this method also
     * deletes its working directory.
     */
    public void destroySolr(String nodeId) {
        stopSolr(nodeId);
        try {
            FileUtils.deleteDirectory(new File(workDir, nodeId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns the {@link JettySolrRunner} identified by the give node ID. */
    public JettySolrRunner getJettySolrRunner(String nodeId) {
        final JettySolrRunner solrRunner = solrRunners.get(nodeId);

        if (solrRunner == null) {
            throw new IllegalArgumentException("No Solr with ID [" + nodeId + "] was started!");
        }
        return solrRunner;
    }

    /** Returns the base URL of the give node. */
    public String getBaseUrl(String nodeId) {
        final JettySolrRunner solrRunner = getJettySolrRunner(nodeId);
        return "http://127.0.0.1:" + solrRunner.getLocalPort() + SOLR_CONTEXT;
    }

    @Override
    public void close() {
        // clone the keys so we don't hit ConcurrentModificationException
        for (String nodeId : Sets.newHashSet(solrRunners.keySet())) {
            destroySolr(nodeId);
        }
    }

    private void startSolrNode(String nodeId) {
        File solrHome = new File(workDir, nodeId);
        if (!solrHome.exists() && !solrHome.mkdirs()) {
            throw new RuntimeException("[" + solrHome + "] does not exist and fails to create");
        }
        try {
            final JettySolrRunner solrRunner = new JettySolrRunner(solrHome.getAbsolutePath(), SOLR_CONTEXT, 0);
            solrRunner.start(true);
            solrRunners.put(nodeId, solrRunner);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

}