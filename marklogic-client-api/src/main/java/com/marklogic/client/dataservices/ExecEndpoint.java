/*
 * Copyright (c) 2019 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.client.dataservices;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.SessionState;
import com.marklogic.client.dataservices.impl.ExecEndpointImpl;
import com.marklogic.client.io.marker.JSONWriteHandle;

import java.io.InputStream;

/**
 * Provides an interface for calling an endpoint that doesn't take
 * input data structures or return output data structures.
 */
@Deprecated
public interface ExecEndpoint extends ExecCaller {
    /**
     * Constructs an instance of the ExecEndpoint interface
     * for calling the specified endpoint.
     * @param client  the database client to use for making calls
     * @param apiDecl  the JSON api declaration specifying how to call the endpoint
     * @return  the ExecEndpoint instance for calling the endpoint
     */
    @Deprecated
    static ExecEndpoint on(DatabaseClient client, JSONWriteHandle apiDecl) {
        final class EndpointLocal<I,O> extends ExecEndpointImpl<I,O> implements ExecEndpoint {
            private EndpointLocal(DatabaseClient client, JSONWriteHandle apiDecl) {
                super(client, apiDecl);
            }
            public ExecEndpoint.BulkExecCaller bulkCaller() {
                return new BulkLocal(this);
            }
            class BulkLocal extends ExecEndpointImpl.BulkExecCallerImpl<I,O>
                    implements ExecEndpoint.BulkExecCaller {
                private BulkLocal(EndpointLocal<I,O> endpoint) {
                    super(endpoint);
                }
            }
        }
        return new EndpointLocal(client, apiDecl);
    }

    /**
     * Makes one call to the endpoint for the instance.
     * @param endpointState  the current mutable state of the endpoint (which must be null if not accepted by the endpoint)
     * @param session  the identifier for the server cache of the endpoint (which must be null if not accepted by the endpoint)
     * @param workUnit  the definition of a unit of work (which must be null if not accepted by the endpoint)
     * @return the endpoint state for the next call, if returned by the endpoint, or null
     */
    @Deprecated
    InputStream call(InputStream endpointState, SessionState session, InputStream workUnit);

    @Override
    @Deprecated
    BulkExecCaller bulkCaller();

    /**
     * Provides an interface for completing a unit of work
     * by repeated calls to an endpoint that doesn't take input
     * data structure or return output data structures.
     *
     * Call awaitCompletion() to start making calls.
     */
    @Deprecated
    interface BulkExecCaller extends ExecCaller.BulkExecCaller {
    }
}
