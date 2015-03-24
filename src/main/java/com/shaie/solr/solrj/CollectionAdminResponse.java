package com.shaie.solr.solrj;

import java.util.Collections;
import java.util.List;

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

/** A base class for collection admin responses. */
public abstract class CollectionAdminResponse extends
        SolrResponse<org.apache.solr.client.solrj.response.CollectionAdminResponse> {

    protected CollectionAdminResponse(org.apache.solr.client.solrj.response.CollectionAdminResponse rawResponse) {
        super(rawResponse);
    }

    /**
     * Returns true if the request was successful. If not, {@link #getErrorMessages()} will return the errors that have
     * occurred.
     */
    public boolean isSuccess() {
        return rawResponse.isSuccess();
    }

    /** Returns the error messages that were returned for the request. */
    public List<String> getErrorMessages() {
        return Collections.emptyList();
    }
}