/*
 * Copyright 2018-2019 MarkLogic Corporation
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
package com.marklogic.client.test.dataservices;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.dataservices.impl.CallManager;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.StringHandle;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

class EndpointUtil {
    private final static Map<String, Format> NODE_FORMATS = new HashMap<>();
    {
        NODE_FORMATS.put("array",          Format.JSON);
        NODE_FORMATS.put("binaryDocument", Format.BINARY);
        NODE_FORMATS.put("jsonDocument",   Format.JSON);
        NODE_FORMATS.put("object",         Format.JSON);
        NODE_FORMATS.put("textDocument",   Format.TEXT);
        NODE_FORMATS.put("xmlDocument",    Format.XML);
    }

    private CallManager callMgr;
    private String endpointDirectory;
    private Map<String, JsonNode> endpointdefs = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();
    private JacksonHandle serviceHandle;

    EndpointUtil(CallManager callMgr, String endpointDirectory) {
        if (callMgr == null)
            throw new IllegalArgumentException("CallManager cannot be null");
        this.callMgr = callMgr;
        if (endpointDirectory == null || endpointDirectory.length()==0)
            throw new IllegalArgumentException("Endpoint Directory cannot be null or empty");
        this.callMgr = callMgr;
        this.endpointDirectory = endpointDirectory;

        ObjectNode servicedef = objectMapper.createObjectNode();
        servicedef.put("endpointDirectory", endpointDirectory);
        this.serviceHandle = new JacksonHandle(servicedef);
    }

    CallManager.CallableEndpoint makeCallableEndpoint(String functionName) {
        return makeCallableEndpoint(functionName, "sjs");
    }
    CallManager.CallableEndpoint makeCallableEndpoint(String functionName, String extension) {
        JsonNode endpointdef = endpointdefs.get(functionName);
        assertNotNull("no endpoint definition found for "+functionName, endpointdef);
        return callMgr.endpoint(serviceHandle, new JacksonHandle(endpointdef), extension);
    }

    CallManager.CallableEndpoint installEndpoint(String functionName) {
        return installEndpoint(functionName, "sjs");
    }
    CallManager.CallableEndpoint installEndpoint(String functionName, String extension) {
        if (functionName == null || functionName.length() == 0)
            throw new IllegalArgumentException("Null or empty function name");
        JsonNode endpointdef = endpointdefs.get(functionName);
        if (endpointdef == null)
            throw new IllegalArgumentException("No endpoint definition of name: "+functionName);
        return callMgr.endpoint(serviceHandle, new JacksonHandle(endpointdef), extension);
    }

    void setupParamNoReturnEndpoint(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String datatype
    ) {
        setupParamNoReturnEndpoint(docMgr, docMeta, functionName, "sjs", datatype);
    }
    void setupParamNoReturnEndpoint(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String extension, String datatype
    ) {
        JsonNode endpointdef = getEndpointdef(functionName, datatype, null, null, false, false);
        String script = getScript(extension, datatype, null, null, false, false);
        setupEndpoint(docMgr, docMeta, endpointdef, extension, script);
    }
    void setupNoParamReturnEndpoint(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String datatype, String returnVal
    ) {
        setupNoParamReturnEndpoint(docMgr, docMeta, functionName, "sjs", datatype, returnVal);
    }
    void setupNoParamReturnEndpoint(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String extension, String datatype, String returnVal
    ) {
        JsonNode endpointdef = getEndpointdef(functionName, null, null, datatype, false, false);
        String script = getScript(extension, null, null, returnVal, false, false);
        setupEndpoint(docMgr, docMeta, endpointdef, extension, script);
    }
    void setupNoParamNoReturnEndpoint(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName
    ) {
        setupNoParamNoReturnEndpoint(docMgr, docMeta, functionName, "sjs");
    }
    void setupNoParamNoReturnEndpoint(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String extension
            ) {
        JsonNode endpointdef = getEndpointdef(functionName, null, null, null, false, false);
        String script = getScript(extension, null, null, null, false, false);
        setupEndpoint(docMgr, docMeta, endpointdef, extension, script);
    }
    void setupTwoParamEndpoint(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String datatype, String paramType2
    ) {
        setupTwoParamEndpoint(docMgr, docMeta, functionName, "sjs", datatype, paramType2);
    }
    void setupTwoParamEndpoint(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String extension, String datatype, String paramType2
    ) {
        setupTwoParamEndpoint(docMgr, docMeta, functionName, extension, datatype, paramType2, false);
    }
    void setupTwoParamEndpoint(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String datatype,
            String paramType2, boolean isMultiple
    ) {
        setupTwoParamEndpoint(docMgr, docMeta, functionName, "sjs", datatype, paramType2, isMultiple);
    }
    void setupTwoParamEndpoint(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String extension, String datatype,
            String paramType2, boolean isMultiple
    ) {
        JsonNode endpointdef = getEndpointdef(functionName, datatype, paramType2, datatype, isMultiple, false);
        String script = getScript(extension, datatype, paramType2, null, isMultiple, false);
        setupEndpoint(docMgr, docMeta, endpointdef, extension, script);
    }
    void setupTwoDifferentParamEndpoint(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String datatype,
            String paramType2, boolean isMultiple, boolean isNullable
    ) {
        setupTwoDifferentParamEndpoint(docMgr, docMeta, functionName, "sjs", datatype, paramType2, isMultiple, isNullable);
    }
    void setupTwoDifferentParamEndpoint(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String extension, String datatype,
            String paramType2, boolean isMultiple, boolean isNullable
    ) {
        JsonNode endpointdef = getEndpointdefWithDifferentParams(functionName, datatype, paramType2, datatype, isMultiple, isNullable);
        String script = getScript(extension, datatype, paramType2, null, isMultiple, false);
        setupEndpoint(docMgr, docMeta, endpointdef, extension, script);
    }
    void setupEndpointSingleNulled(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String datatype
    ) {
        setupEndpointSingleNulled(docMgr, docMeta, functionName, "sjs", datatype);
    }
    void setupEndpointSingleNulled(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String extension, String datatype
    ) {
        JsonNode endpointdef = getEndpointdef(functionName, datatype, false, true);
        String script = getScript(extension, datatype, false, true);
        setupEndpoint(docMgr, docMeta, endpointdef, extension, script);
    }
    void setupEndpointSingleRequired(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String datatype
    ) {
        setupEndpointSingleRequired(docMgr, docMeta, functionName, "sjs", datatype);
    }
    void setupEndpointSingleRequired(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String extension, String datatype
    ) {
        JsonNode endpointdef = getEndpointdef(functionName, datatype, false, false);
        String script = getScript(extension, datatype, false, false);
        setupEndpoint(docMgr, docMeta, endpointdef, extension, script);
    }
    void setupEndpointMultipleNulled(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String datatype
    ) {
        setupEndpointMultipleNulled(docMgr, docMeta, functionName, "sjs", datatype);
    }
    void setupEndpointMultipleNulled(
            JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName, String extension, String datatype
    ) {
        JsonNode endpointdef = getEndpointdef(functionName, datatype, true, true);
        String script = getScript(extension, datatype, true, true);
        setupEndpoint(docMgr, docMeta, endpointdef, extension, script);
    }
    void setupEndpointMultipleRequired(JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String datatype) {
        setupEndpointMultipleRequired(docMgr, docMeta, datatype, "sjs", datatype);
    }
    void setupEndpointMultipleRequired(JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String extension, String datatype) {
        setupEndpointMultipleRequired(docMgr, docMeta, datatype, "sjs", datatype);
    }
    void setupEndpointMultipleRequired(JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName,
            String extension, String datatype) {
        JsonNode endpointdef = getEndpointdef(functionName, datatype, true, false);
        String script = getScript(extension, datatype, true, false);
        setupEndpoint(docMgr, docMeta, endpointdef, extension, script);
    }
    void setupEndpoint(JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, JsonNode endpointdef, String extension, String script) {
        String functionName = endpointdef.get("functionName").asText();
        String baseUri      = endpointDirectory + functionName;
        docMgr.write(baseUri+".api", docMeta, new JacksonHandle(endpointdef));
        docMgr.write(baseUri+"."+extension, docMeta, new StringHandle(script));

        endpointdefs.put(functionName, endpointdef);
    }
    void setupSingleEndpointWithForestParam( JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName,
            String datatype, String paramType2, boolean isMultiple, boolean isNullable) {
        setupSingleEndpointWithForestParam(
                docMgr, docMeta, functionName, "sjs", datatype, paramType2, isMultiple, isNullable
        );
    }
    void setupSingleEndpointWithForestParam( JSONDocumentManager docMgr, DocumentMetadataHandle docMeta, String functionName,
            String extension, String datatype, String paramType2, boolean isMultiple, boolean isNullable) {
        JsonNode endpointdef = getEndpointdefWithForestParamName(functionName, datatype, paramType2, datatype, isMultiple, isNullable);
        String script = getScriptWithForestParam(extension, datatype, paramType2, datatype, isMultiple, false);
        setupEndpoint(docMgr, docMeta, endpointdef, extension, script);
    }

    JsonNode getEndpointdef(String functionName, String datatype, boolean isMultiple, boolean isNullable) {
        return getEndpointdef(functionName, datatype, null, datatype, isMultiple, isNullable);
    }
    JsonNode getEndpointdef(
            String functionName, String paramType1, String paramType2, String returnType, boolean isMultiple, boolean isNullable
    ) {
        ObjectNode endpointdef = objectMapper.createObjectNode();
        endpointdef.put("functionName", functionName);
        if (paramType1 != null) {
            ArrayNode paramdefs  = objectMapper.createArrayNode();
            ObjectNode paramdef = objectMapper.createObjectNode();
            paramdef.put("name", "param1");
            paramdef.put("datatype", paramType1);
            paramdef.put("multiple", isMultiple);
            paramdef.put("nullable", isNullable);
            paramdefs.add(paramdef);
            if (paramType2 != null) {
                paramdef = objectMapper.createObjectNode();
                paramdef.put("name", "param2");
                paramdef.put("datatype", paramType2);
                paramdef.put("multiple", !isMultiple);
                paramdef.put("nullable", isNullable);
                paramdefs.add(paramdef);
            }
            endpointdef.set("params", paramdefs);
        }
        if (returnType != null) {
            ObjectNode returndef = objectMapper.createObjectNode();
            returndef.put("datatype", returnType);
            returndef.put("multiple", isMultiple);
            returndef.put("nullable", isNullable);
            endpointdef.set("return", returndef);
        }
        return endpointdef;
    }
    
    JsonNode getEndpointdefWithDifferentParams(
            String functionName, String paramType1, String paramType2, String returnType, boolean isMultiple, boolean isNullable
    ) {
        ObjectNode endpointdef = objectMapper.createObjectNode();
        endpointdef.put("functionName", functionName);
        if (paramType1 != null) {
            ArrayNode paramdefs  = objectMapper.createArrayNode();
            ObjectNode paramdef = objectMapper.createObjectNode();
            paramdef.put("name", "param1");
            paramdef.put("datatype", paramType1);
            paramdef.put("multiple", isMultiple);
            paramdef.put("nullable", isNullable);
            paramdefs.add(paramdef);
            if (paramType2 != null) {
                paramdef = objectMapper.createObjectNode();
                paramdef.put("name", "param2");
                paramdef.put("datatype", paramType2);
                paramdef.put("multiple", !isMultiple);
                paramdef.put("nullable", !isNullable);
                paramdefs.add(paramdef);
            }
            endpointdef.set("params", paramdefs);
        }
        if (returnType != null) {
            ObjectNode returndef = objectMapper.createObjectNode();
            returndef.put("datatype", returnType);
            returndef.put("multiple", isMultiple);
            returndef.put("nullable", isNullable);
            endpointdef.set("return", returndef);
        }
        return endpointdef;
    }
    
    JsonNode getEndpointdefWithForestParamName(
            String functionName, String paramType1, String paramType2, String returnType, boolean isMultiple, boolean isNullable
    ) {
        ObjectNode endpointdef = objectMapper.createObjectNode();
        endpointdef.put("functionName", functionName);
        if (paramType1 != null) {
            ArrayNode paramdefs  = objectMapper.createArrayNode();
            ObjectNode paramdef = objectMapper.createObjectNode();
            paramdef.put("name", "forestParamName");
            paramdef.put("datatype", paramType1);
            paramdef.put("multiple", isMultiple);
            paramdef.put("nullable", isNullable);
            paramdefs.add(paramdef);
            if (paramType2 != null) {
                paramdef = objectMapper.createObjectNode();
                paramdef.put("name", "forestParamName2");
                paramdef.put("datatype", paramType2);
                paramdef.put("multiple", isMultiple);
                paramdef.put("nullable", !isNullable);
                paramdefs.add(paramdef);
            }
            endpointdef.set("params", paramdefs);
        }
        if (returnType != null) {
            ObjectNode returndef = objectMapper.createObjectNode();
            returndef.put("datatype", returnType);
            returndef.put("multiple", isMultiple);
            returndef.put("nullable", isNullable);
            endpointdef.set("return", returndef);
        }
        return endpointdef;
    }

    String getScript(String extension, String datatype, boolean isMultiple, boolean isNullable) {
        return getScript(extension, datatype, null, null, isMultiple, isNullable);
    }

    String getScript(
            String extension, String paramType1, String paramType2, String returnVal, boolean isMultiple, boolean isNullable
    ) {
        StringBuilder scriptBldr = new StringBuilder()
                .append("'use strict';\n");
        if (paramType1 != null) {
            scriptBldr = scriptBldr
                    .append((extension == "sjs") ? "var param1;\n" : "const param1 = external.param1;\n");
            if (paramType2 != null) {
                scriptBldr = scriptBldr
                        .append((extension == "sjs") ? "var param2;\n" : "const param2 = external.param2;\n" );
            }
        }

        if (paramType1 != null) {
            if (isNullable) {
                scriptBldr = scriptBldr
                        .append("if (fn.count(param1) != 0)\n")
                        .append("  fn.error(null, 'TEST_ERROR',\n")
                        .append("    'received ' + fn.count(param1) + ' instead of no values');\n");
            } else if (isMultiple) {
                scriptBldr = scriptBldr
                        .append("if (fn.count(param1) < 2)\n")
                        .append("  fn.error(null, 'TEST_ERROR',\n")
                        .append("    'received ' + fn.count(param1) + ' instead of multiple values');\n")
                        .append("const value1 = fn.head(param1);\n");
            } else {
                scriptBldr = scriptBldr
                        .append("const value1 = param1;\n");
            }
            if (paramType2 != null) {
                if (!isMultiple) {
                    scriptBldr = scriptBldr
                            .append("if (fn.count(param2) < 2)\n")
                            .append("  fn.error(null, 'TEST_ERROR',\n")
                            .append("    'received ' + fn.count(param2) + ' instead of multiple values');\n")
                            .append("const value2 = fn.head(param2);\n");
                } else {
                    scriptBldr = scriptBldr
                            .append("const value2 = param2;\n");
                }
            }

            Format documentFormat = isNullable ? null : NODE_FORMATS.get(paramType1);
            if (isNullable) {
                scriptBldr = scriptBldr
                        .append("const isValid = true;\n");
            } else if (documentFormat != null) {
                scriptBldr = scriptBldr
                        .append("const isValid = ((value1 instanceof Document) ?\n")
                        .append("    value1.documentFormat == '").append(documentFormat.name()).append("' :\n")
                        .append("    xdmp.nodeKind(value1) == '").append(paramType1).append("'\n")
                        .append("    );\n");
            } else {
                scriptBldr = scriptBldr
                        .append("const isValid = (\n")
                        .append("    fn.localNameFromQName(xdmp.type(value1)) == '").append(paramType1).append("' ||\n")
                        .append("    xdmp.castableAs('http://www.w3.org/2001/XMLSchema', '").append(paramType1).append("', value1)\n")
                        .append("    );\n");
            }
            if (paramType2 != null) {
                Format documentFormat2 = isNullable ? null : NODE_FORMATS.get(paramType2);
                if (documentFormat2 != null) {
                    scriptBldr = scriptBldr
                            .append("const isValid2 = ((value2 instanceof Document) ?\n")
                            .append("    value2.documentFormat == '").append(documentFormat2.name()).append("' :\n")
                            .append("    xdmp.nodeKind(value2) == '").append(paramType2).append("'\n")
                            .append("    );\n");
                } else {
                    scriptBldr = scriptBldr
                            .append("const isValid2 = (\n")
                            .append("    fn.localNameFromQName(xdmp.type(value2)) == '").append(paramType2).append("' ||\n")
                            .append("    xdmp.castableAs('http://www.w3.org/2001/XMLSchema', '").append(paramType2).append("', value2)\n")
                            .append("    );\n");
                }
            }

            scriptBldr = scriptBldr
                    .append("if (!isValid)\n")
                    .append("  fn.error(null, 'TEST_ERROR',\n")
                    .append("    'param1 set to ' + Object.prototype.toString.call(value1) +")
                    .append("    ' instead of ").append(paramType1).append(" value');\n");
            if (paramType2 != null) {
                scriptBldr = scriptBldr
                        .append("if (!isValid2)\n")
                        .append("  fn.error(null, 'TEST_ERROR',\n")
                        .append("    'param2 set to ' + Object.prototype.toString.call(value2) +")
                        .append("    ' instead of ").append(paramType2).append(" value');\n");
            }

            scriptBldr = scriptBldr
                    .append("param1;");
        } else if (returnVal != null) {
            scriptBldr = scriptBldr
                    .append(returnVal)
                    .append(";");
        }

        return scriptBldr.toString();
    }
    
    String getScriptWithForestParam(
            String extension, String paramType1, String paramType2, String returnVal, boolean isMultiple, boolean isNullable
    ) {
        StringBuilder scriptBldr = new StringBuilder()
                .append("'use strict';\n");
        if (paramType1 != null) {
            scriptBldr = scriptBldr
                    .append((extension == "sjs") ? "var forestParamName;\n" : "const forestParamName = external.forestParamName;\n");
            if (paramType2 != null) {
                scriptBldr = scriptBldr
                        .append((extension == "sjs") ? "var forestParamName2;\n" : "const forestParamName2 = external.forestParamName2;\n");
            }
        }

        if (paramType1 != null) {
            if (isNullable) {
                scriptBldr = scriptBldr
                        .append("if (fn.count(forestParamName) != 0)\n")
                        .append("  fn.error(null, 'TEST_ERROR',\n")
                        .append("    'received ' + fn.count(forestParamName) + ' instead of no values');\n");
            } else if (isMultiple) {
                scriptBldr = scriptBldr
                        .append("if (fn.count(forestParamName) < 2)\n")
                        .append("  fn.error(null, 'TEST_ERROR',\n")
                        .append("    'received ' + fn.count(forestParamName) + ' instead of multiple values forestParamName');\n")
                        .append("const value1 = fn.head(forestParamName);\n");
            } else {
                scriptBldr = scriptBldr
                        .append("const value1 = forestParamName;\n");
            }
            if (paramType2 != null) {
                if (isMultiple) {
                    scriptBldr = scriptBldr
                            .append("if (fn.count(forestParamName2) < 2)\n")
                            .append("  fn.error(null, 'TEST_ERROR',\n")
                            .append("    'received ' + fn.count(forestParamName2) + ' instead of multiple values forestParamName2');\n")
                            .append("const value2 = fn.head(forestParamName2);\n");
                } else {
                    scriptBldr = scriptBldr
                            .append("const value2 = forestParamName2;\n");
                }
            }

            Format documentFormat = isNullable ? null : NODE_FORMATS.get(paramType1);
            if (isNullable) {
                scriptBldr = scriptBldr
                        .append("const isValid = true;\n");
            } else if (documentFormat != null) {
                scriptBldr = scriptBldr
                        .append("const isValid = ((value1 instanceof Document) ?\n")
                        .append("    value1.documentFormat == '").append(documentFormat.name()).append("' :\n")
                        .append("    xdmp.nodeKind(value1) == '").append(paramType1).append("'\n")
                        .append("    );\n");
            } else {
                scriptBldr = scriptBldr
                        .append("const isValid = (\n")
                        .append("    fn.localNameFromQName(xdmp.type(value1)) == '").append(paramType1).append("' ||\n")
                        .append("    xdmp.castableAs('http://www.w3.org/2001/XMLSchema', '").append(paramType1).append("', value1)\n")
                        .append("    );\n");
            }
            if (paramType2 != null) {
                Format documentFormat2 = isNullable ? null : NODE_FORMATS.get(paramType2);
                if (documentFormat2 != null) {
                    scriptBldr = scriptBldr
                            .append("const isValid2 = ((value2 instanceof Document) ?\n")
                            .append("    value2.documentFormat == '").append(documentFormat2.name()).append("' :\n")
                            .append("    xdmp.nodeKind(value2) == '").append(paramType2).append("'\n")
                            .append("    );\n");
                } else {
                    scriptBldr = scriptBldr
                            .append("const isValid2 = (\n")
                            .append("    fn.localNameFromQName(xdmp.type(value2)) == '").append(paramType2).append("' ||\n")
                            .append("    xdmp.castableAs('http://www.w3.org/2001/XMLSchema', '").append(paramType2).append("', value2)\n")
                            .append("    );\n");
                }
            }

            scriptBldr = scriptBldr
                    .append("if (!isValid)\n")
                    .append("  fn.error(null, 'TEST_ERROR',\n")
                    .append("    'forestParamName set to ' + Object.prototype.toString.call(value1) +")
                    .append("    ' instead of ").append(paramType1).append(" value');\n");
            if (paramType2 != null) {
                scriptBldr = scriptBldr
                        .append("if (!isValid2)\n")
                        .append("  fn.error(null, 'TEST_ERROR',\n")
                        .append("    'forestParamName2 set to ' + Object.prototype.toString.call(value2) +")
                        .append("    ' instead of ").append(paramType2).append(" value');\n");
            }

            scriptBldr = scriptBldr
                    .append("forestParamName;");
        } else if (returnVal != null) {
            scriptBldr = scriptBldr
                    .append(returnVal)
                    .append(";");
        }

        return scriptBldr.toString();
    }

    <T> CallManager.ManyCaller<T> makeManyCaller(CallManager.CallableEndpoint callableEndpoint, Class<T> as) {
        return callableEndpoint.returningMany(as);
    }

    <T> CallManager.OneCaller<T> makeOneCaller(CallManager.CallableEndpoint callableEndpoint, Class<T> as) {
        return callableEndpoint.returningOne(as);
    }
}