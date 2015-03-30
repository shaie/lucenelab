package com.shaie.solr.utils;

import java.io.IOException;

import org.apache.curator.test.TestingServer;
import org.junit.rules.ExternalResource;

import com.google.common.base.Throwables;
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

public class TestingServerResource extends ExternalResource {

    private final TestingServer zkServer;

    public TestingServerResource() {
        try {
            zkServer = new TestingServer(false);
            System.setProperty(SolrCloudUtils.ZK_HOST_PROP_NAME, zkServer.getConnectString());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected void before() throws Throwable {
        zkServer.start();
    }

    @Override
    protected void after() {
        try {
            zkServer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getConnectString() {
        return zkServer.getConnectString();
    }

}