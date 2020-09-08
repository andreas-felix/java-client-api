/*
 * Copyright (c) 2020 MarkLogic Corporation
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
package com.marklogic.client.dataservices.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.SessionState;
import com.marklogic.client.impl.BaseProxy;
import com.marklogic.client.impl.NodeConverter;
import com.marklogic.client.impl.RESTServices;
import com.marklogic.client.io.BaseHandle;
import com.marklogic.client.io.BytesHandle;
import com.marklogic.client.io.marker.BufferableContentHandle;
import com.marklogic.client.io.marker.JSONWriteHandle;

import java.util.stream.Stream;

abstract class IOCallerImpl<I,O> extends BaseCallerImpl {
    private final JsonNode                    apiDeclaration;
    private final String                      endpointPath;
    private final BaseProxy.DBFunctionRequest requester;

    private BufferableContentHandle<I,?> inputHandle;
    private BufferableContentHandle<O,?> outputHandle;

    private ParamdefImpl  endpointStateParamdef;
    private ParamdefImpl  sessionParamdef;
    private ParamdefImpl  endpointConstantsParamdef;
    private ParamdefImpl  inputParamdef;
    private ReturndefImpl returndef;

    IOCallerImpl(
            JSONWriteHandle apiDeclaration, BufferableContentHandle<I,?> inputHandle, BufferableContentHandle<O,?> outputHandle
    ) {
        super();

        if (apiDeclaration== null) {
            throw new IllegalArgumentException("null endpoint declaration");
        }

        this.apiDeclaration = NodeConverter.handleToJsonNode(apiDeclaration);
        if (!this.apiDeclaration.isObject()) {
            throw new IllegalArgumentException(
                    "endpoint declaration must be object: " + this.apiDeclaration.toString()
            );
        }

        this.endpointPath = getText(this.apiDeclaration.get("endpoint"));
        if (this.endpointPath == null || this.endpointPath.length() == 0) {
            throw new IllegalArgumentException(
                    "no endpoint in endpoint declaration: " + this.apiDeclaration.toString()
            );
        }

        int nodeArgCount = 0;

        JsonNode functionParams = this.apiDeclaration.get("params");
        if (functionParams != null) {
            if (!functionParams.isArray()) {
                throw new IllegalArgumentException(
                        "params must be array in endpoint declaration: " + this.apiDeclaration.toString()
                );
            }

            int paramCount = functionParams.size();
            if (paramCount > 0) {
                for (JsonNode functionParam : functionParams) {
                    if (!functionParam.isObject()) {
                        throw new IllegalArgumentException(
                                "parameter must be object in endpoint declaration: " + functionParam.toString()
                        );
                    }
                    ParamdefImpl paramdef = new ParamdefImpl(functionParam);

                    String paramName = paramdef.getParamName();
                    switch(paramName) {
                        case "endpointState":
                            if (paramdef.isMultiple()) {
                                throw new IllegalArgumentException("endpointState parameter cannot be multiple");
                            } else if (!paramdef.isNullable()) {
                                throw new IllegalArgumentException("endpointState parameter must be nullable");
                            }
                            this.endpointStateParamdef = paramdef;
                            nodeArgCount++;
                            break;
                        case "input":
                            if (!paramdef.isMultiple()) {
                                throw new IllegalArgumentException("input parameter must be multiple");
                            } else if (!paramdef.isNullable()) {
                                throw new IllegalArgumentException("input parameter must be nullable");
                            }
                            this.inputParamdef = paramdef;
                            if (inputHandle == null) {
                                throw new IllegalArgumentException("no input handle provided for input parameter");
                            }
                            ((BaseHandle) inputHandle).setFormat(paramdef.getFormat());
                            this.inputHandle  = inputHandle;
                            nodeArgCount += 2;
                            break;
                        case "session":
                            if (!"session".equalsIgnoreCase(paramdef.getDataType())) {
                                throw new IllegalArgumentException("session parameter must have session data type");
                            } else if (paramdef.isMultiple()) {
                                throw new IllegalArgumentException("session parameter cannot be multiple");
                            }
                            this.sessionParamdef = paramdef;
                            break;
                        case "endpointConstants":
                        case "workUnit":
                            if (this.endpointConstantsParamdef != null) {
                                throw new IllegalArgumentException("can only declare one of "+paramName+" and "+
                                        this.endpointConstantsParamdef.getParamName());
                            } else if (paramdef.isMultiple()) {
                                throw new IllegalArgumentException(paramName+" parameter cannot be multiple");
                            }
                            this.endpointConstantsParamdef  = paramdef;
                            nodeArgCount++;
                            break;
                        default:
                            throw new IllegalArgumentException("unknown parameter name: "+paramName);
                    }
                }
            }
        }
        if (this.inputParamdef == null && inputHandle != null) {
            throw new IllegalArgumentException("no input parameter declared but input handle provided");
        }

        JsonNode functionReturn = this.apiDeclaration.get("return");
        if (functionReturn != null) {
            if (!functionReturn.isObject()) {
                throw new IllegalArgumentException(
                        "return must be object in endpoint declaration: "+functionReturn.toString()
                );
            }
            this.returndef = new ReturndefImpl(functionReturn);
            if (!this.returndef.isNullable()) {
                throw new IllegalArgumentException("return must be nullable");
            }
            if (outputHandle != null) {
                ((BaseHandle) outputHandle).setFormat(this.returndef.getFormat());
                this.outputHandle = outputHandle;
            } else if (this.endpointStateParamdef == null) {
                throw new IllegalArgumentException("no output handle provided for return values");
            }
        } else if (outputHandle != null) {
            throw new IllegalArgumentException("no return values declared but output handle provided");
        }

        if (this.endpointStateParamdef != null) {
            if (this.returndef == null) {
                throw new IllegalArgumentException(
                        "endpointState parameter requires return in endpoint: "+getEndpointPath()
                );
            } else if (this.endpointStateParamdef.getFormat() != this.returndef.getFormat()) {
                throw new IllegalArgumentException(
                        "endpointState format must match return format in endpoint: "+getEndpointPath()
                );
            }
        }

        this.requester = BaseProxy.moduleRequest(
                getEndpointPath(), BaseProxy.ParameterValuesKind.forNodeCount(nodeArgCount)
        );
    }

    BufferableContentHandle<I, ?> getInputHandle() {
        return inputHandle;
    }
    BufferableContentHandle<O, ?> getOutputHandle() {
        return outputHandle;
    }

    BaseProxy.DBFunctionRequest makeRequest(DatabaseClient db, CallContextImpl<I,O> callCtxt) {
        return makeRequest(db, callCtxt, (RESTServices.CallField) null);
    }
    BaseProxy.DBFunctionRequest makeRequest(
            DatabaseClient db, CallContextImpl<I,O> callCtxt, Stream<I> input
    ) {
        RESTServices.CallField inputField = null;

        ParamdefImpl paramdef = getInputParamdef();
        if (paramdef != null) {
            inputField = BaseProxy.documentParam(
                    "input",
                    paramdef.isNullable(),
                    NodeConverter.streamWithFormat(input.map(inputHandle::resendableHandleFor), paramdef.getFormat())
                    );
        } else if (input != null) {
            throw new IllegalArgumentException("input parameter not supported by endpoint: "+getEndpointPath());
        }

        return makeRequest(db, callCtxt, inputField);
    }
    BaseProxy.DBFunctionRequest makeRequest(
            DatabaseClient db, CallContextImpl<I,O> callCtxt, I[] input
    ) {
        RESTServices.CallField inputField = null;

        ParamdefImpl paramdef = getInputParamdef();
        if (paramdef != null) {
            inputField = BaseProxy.documentParam(
                    "input",
                    paramdef.isNullable(),
                    NodeConverter.arrayWithFormat(inputHandle.resendableHandleFor(input), paramdef.getFormat())
            );
        } else if (input != null && input.length > 0) {
            throw new IllegalArgumentException("input parameter not supported by endpoint: "+getEndpointPath());
        }

        return makeRequest(db, callCtxt, inputField);
    }
    private BaseProxy.DBFunctionRequest makeRequest(
            DatabaseClient db, CallContextImpl<I,O> callCtxt, RESTServices.CallField inputField
    ) {
        BaseProxy.DBFunctionRequest request = getRequester().on(db);

        SessionState session = callCtxt.getSessionState();
        if (getSessionParamdef() != null) {
            request = request.withSession(
                    getSessionParamdef().getParamName(), session, getSessionParamdef().isNullable()
            );
        } else if (session != null) {
            throw new IllegalArgumentException("session not supported by endpoint: "+getEndpointPath());
        }

        int fieldNum = 0;

        RESTServices.CallField endpointStateField = null;
        BytesHandle endpointState = callCtxt.getEndpointState();
        if (getEndpointStateParamdef() != null) {
            endpointStateField = BaseProxy.documentParam(
                    "endpointState",
                    getEndpointStateParamdef().isNullable(),
                    NodeConverter.withFormat(endpointState, getEndpointStateParamdef().getFormat())
                    );
            if (endpointState != null)
                fieldNum++;
        } else if (endpointState != null) {
            throw new IllegalArgumentException("endpointState parameter not supported by endpoint: "+getEndpointPath());
        }

        RESTServices.CallField endpointConstantsField = null;
        BytesHandle endpointConstants = callCtxt.getEndpointConstants();
        if (getEndpointConstantsParamdef() != null) {
            endpointConstantsField = BaseProxy.documentParam(
                    getEndpointConstantsParamdef().getParamName(),
                    getEndpointConstantsParamdef().isNullable(),
                    NodeConverter.withFormat(endpointConstants, getEndpointConstantsParamdef().getFormat())
                    );
            if (endpointConstants != null)
                fieldNum++;
        } else if (endpointConstants != null) {
            throw new IllegalArgumentException(callCtxt.getEndpointConstantsParamName()+
                    " parameter not supported by endpoint: "+getEndpointPath());
        }

        if (inputField != null)
            fieldNum++;

        if (fieldNum > 0) {
            RESTServices.CallField[] fields = new RESTServices.CallField[fieldNum];
            fieldNum = 0;
            if (endpointStateField != null) {
                fields[fieldNum++] = endpointStateField;
            }
            if (endpointConstantsField != null) {
                fields[fieldNum++] = endpointConstantsField;
            }
            if (inputField != null) {
                fields[fieldNum++] = inputField;
            }

            request = request.withParams(fields);
        }

        return request;
    }
    boolean responseWithState(BaseProxy.DBFunctionRequest request, CallContextImpl<I,O> callCtxt) {
        if (getReturndef() == null) {
            request.responseNone();
            return false;
        } else if (getReturndef().isMultiple()) {
            throw new UnsupportedOperationException("multiple return from endpoint: "+getEndpointPath());
        }

        return request.responseSingle(getReturndef().isNullable(), getReturndef().getFormat())
               .asEndpointState(callCtxt.getEndpointState());
    }
    O responseSingle(BaseProxy.DBFunctionRequest request) {
        if (getReturndef() == null) {
            throw new UnsupportedOperationException("no return from endpoint: "+getEndpointPath());
        } else if (getReturndef().isMultiple()) {
            throw new UnsupportedOperationException("multiple return from endpoint: "+getEndpointPath());
        }

        return request.responseSingle(getReturndef().isNullable(), getReturndef().getFormat()).asContent(outputHandle);
    }
    Stream<O> responseMultipleAsStream(BaseProxy.DBFunctionRequest request, CallContextImpl<I,O> callCtxt) {
        return responseMultiple(request).asStreamOfContent(callCtxt.isLegacyContext() ? null : callCtxt.getEndpointState(), outputHandle);
    }
    O[] responseMultipleAsArray(BaseProxy.DBFunctionRequest request, CallContextImpl<I,O> callCtxt) {
        return responseMultiple(request).asArrayOfContent(callCtxt.isLegacyContext() ? null : callCtxt.getEndpointState(), outputHandle);
    }
    private RESTServices.MultipleCallResponse responseMultiple(BaseProxy.DBFunctionRequest request) {
        if (getReturndef() == null) {
            throw new UnsupportedOperationException("no return from endpoint: "+getEndpointPath());
        } else if (!getReturndef().isMultiple()) {
            throw new UnsupportedOperationException("single return from endpoint: "+getEndpointPath());
        }

        return request.responseMultiple(getReturndef().isNullable(), getReturndef().getFormat());
    }

    JsonNode getApiDeclaration() {
        return this.apiDeclaration;
    }

    String getEndpointPath() {
        return this.endpointPath;
    }

    ParamdefImpl getEndpointStateParamdef() {
        return this.endpointStateParamdef;
    }
    ParamdefImpl getSessionParamdef() {
        return this.sessionParamdef;
    }
    ParamdefImpl getEndpointConstantsParamdef() {
        return this.endpointConstantsParamdef;
    }
    ParamdefImpl getInputParamdef() {
        return this.inputParamdef;
    }
    ReturndefImpl getReturndef() {
        return this.returndef;
    }
    BaseProxy.DBFunctionRequest getRequester() {
        return this.requester;
    }
}
