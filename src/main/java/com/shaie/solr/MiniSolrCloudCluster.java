package com.shaie.solr;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.embedded.JettySolrRunner;

import com.carrotsearch.ant.tasks.junit4.dependencies.com.google.common.base.Throwables;

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

/** Simulates a SolrCloud cluster by creating allowing to start/stop nodes. */
public class MiniSolrCloudCluster implements AutoCloseable {

    private final File workDir;
    private final Map<String, JettySolrRunner> solrRunners = new HashMap<>();

    public MiniSolrCloudCluster(File workDir) {
        this.workDir = workDir;
    }

    /** Starts multiple Solr nodes. */
    public void startSolrNodes(String... nodeIDs) {
        for (String nodeId : nodeIDs) {
            startSolrNode(nodeId);
        }
    }

    /** Stops the Solr identified by the given {@code nodeId}. */
    public void stopSolr(String nodeId) {
        JettySolrRunner solrRunner = solrRunners.get(nodeId);

        if (solrRunner == null) {
            throw new IllegalArgumentException("No Solr with ID [" + nodeId + "] was started!");
        }

        try {
            solrRunner.stop();
            solrRunners.remove(nodeId);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void close() {
        Throwable t = null;
        for (JettySolrRunner solrRunner : solrRunners.values()) {
            try {
                solrRunner.stop();
            } catch (Exception e) {
                if (t == null) {
                    t = e;
                } else {
                    t.addSuppressed(e);
                }
            }
        }
        if (t != null) {
            throw Throwables.propagate(t);
        }
        solrRunners.clear();
    }

    private void startSolrNode(String nodeId) {
        File solrHome = new File(workDir, nodeId);
        if (!solrHome.exists() && !solrHome.mkdirs()) {
            throw new RuntimeException("[" + solrHome + "] does not exist and fails to create");
        }
        try {
            final JettySolrRunner solrRunner = new JettySolrRunner(solrHome.getAbsolutePath(), "/solr", 0);
            solrRunner.start(true);
            solrRunners.put(nodeId, solrRunner);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

}