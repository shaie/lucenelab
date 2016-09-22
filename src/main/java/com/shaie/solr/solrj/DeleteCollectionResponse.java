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
package com.shaie.solr.solrj;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.solr.common.util.NamedList;

public class DeleteCollectionResponse extends CollectionAdminResponse {

    private final List<String> coreNames;

    public DeleteCollectionResponse(org.apache.solr.client.solrj.response.CollectionAdminResponse rawResponse) {
        super(rawResponse);

        if (isSuccess()) {
            coreNames = new ArrayList<>();
            for (final Entry<String, Object> entry : getSuccessEntry(rawResponse)) {
                coreNames.add(entry.getKey());
            }
        } else {
            coreNames = null;
        }
    }

    /** Returns the cores that were created */
    public List<String> getCoreNames() {
        return coreNames;
    }

    @SuppressWarnings("unchecked")
    private static NamedList<Object> getSuccessEntry(
            org.apache.solr.client.solrj.response.CollectionAdminResponse rawResponse) {
        return (NamedList<Object>) rawResponse.getResponse().get("success");
    }

}