/*
 * swagger-hub
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.swagger.parser

import com.blackducksoftware.integration.swagger.model.ApiPath
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class SwaggerPathsParser {
    public List<ApiPath> getPathsToResponses(JsonObject swaggerJson, Map<String, String> overrideEntries) {
        def apiPaths = []

        final JsonObject definitions = swaggerJson.get('paths').getAsJsonObject();
        for (final Map.Entry<String, JsonElement> entry : definitions.entrySet()) {
            String path = entry.key
            if (!'/api/'.equals(path) && !path.contains('{') &&!path.contains('}')) {
                if (entry.value.asJsonObject.has('get')) {
                    JsonObject get = entry.value.asJsonObject.getAsJsonObject('get')
                    if (get.has('responses')) {
                        JsonObject responses = get.getAsJsonObject('responses')
                        if (responses.has('200')) {
                            JsonObject twoHundredResponses = responses.getAsJsonObject('200')
                            if (twoHundredResponses.has('schema')) {
                                JsonObject schema = twoHundredResponses.getAsJsonObject('schema')
                                if (schema.has('$ref')) {
                                    String ref = schema.get('$ref').asString
                                    int lastSlashIndex = ref.lastIndexOf('/')
                                    int lastQuoteIndex = ref.lastIndexOf('"')
                                    String resultClass = ref[(lastSlashIndex + 1)..lastQuoteIndex]
                                    boolean hasManyResults = false
                                    if (resultClass.contains(SwaggerDefinitionsParser.CONTAINER_START_MARKER) && resultClass.contains(SwaggerDefinitionsParser.CONTAINER_END_MARKER)) {
                                        int start = resultClass.lastIndexOf(SwaggerDefinitionsParser.CONTAINER_START_MARKER) + 1
                                        int end = resultClass.indexOf(SwaggerDefinitionsParser.CONTAINER_END_MARKER) - 1
                                        resultClass = resultClass[start..end]
                                        hasManyResults = true
                                    }
                                    if (overrideEntries.containsKey(path)) {
                                        resultClass = overrideEntries.get(path)
                                    }
                                    apiPaths.add(new ApiPath(path, hasManyResults, resultClass))
                                }
                            }
                        }
                    }
                }
            }
        }
        return apiPaths
    }
}
