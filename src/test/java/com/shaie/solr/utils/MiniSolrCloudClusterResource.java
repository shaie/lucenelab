package com.shaie.solr.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.curator.test.TestingServer;
import org.junit.rules.ExternalResource;

import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.shaie.solr.MiniSolrCloudCluster;
import com.shaie.solr.SolrCloudUtils;

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

/** Manages a {@link MiniSolrCloudCluster} and a {@link TestingServer}. */
public class MiniSolrCloudClusterResource extends ExternalResource {

    private final TestingServer zkServer;
    private final MiniSolrCloudCluster solrCluster;
    private final File workDir;

    public MiniSolrCloudClusterResource(File solrXml) {
        workDir = Files.createTempDir();
        zkServer = startZooKeeper(new File(workDir, "zookeeper"));
        solrCluster = new MiniSolrCloudCluster(new File(workDir, "solr"), solrXml, zkServer.getConnectString());
    }

    @Override
    protected void after() {
        try {
            solrCluster.close();
            zkServer.close();
            FileUtils.deleteDirectory(workDir);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MiniSolrCloudCluster getSolrCluster() {
        return solrCluster;
    }

    public File getWorkDir() {
        return workDir;
    }

    public String getConnectString() {
        return zkServer.getConnectString();
    }

    private TestingServer startZooKeeper(File workDir) {
        try {
            final TestingServer zkServer = new TestingServer(-1, workDir, false);
            zkServer.start();
            System.setProperty(SolrCloudUtils.ZK_HOST_PROP_NAME, zkServer.getConnectString());
            return zkServer;
        } catch (final Exception e) {
            throw Throwables.propagate(e);
        }
    }

}