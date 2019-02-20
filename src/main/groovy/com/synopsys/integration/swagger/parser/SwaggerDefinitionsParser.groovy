/*
 * swagger-hub
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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
package com.synopsys.integration.swagger.parser

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.synopsys.integration.swagger.model.DefinitionsWithManyVersions
import com.synopsys.integration.swagger.model.SwaggerDefinition
import com.synopsys.integration.swagger.model.SwaggerDefinitionProperty

public class SwaggerDefinitionsParser {
    public static final String CONTAINER_START_MARKER = '\u00ab'
    public static final String CONTAINER_END_MARKER = '\u00bb'
    public static final List<String> DEFINITIONS_TO_IGNORE_AND_CREATE_MANUALLY = ['boolean',
                                                                                  'string',
                                                                                  'DateTime',
                                                                                  'string,RiskPriorityDistribution',
                                                                                  'JsonNode']

    private final SwaggerPropertiesParser swaggerPropertiesParser
    private final DefinitionsWithManyVersions definitionsWithManyVersions

    def definitionNamesReferencedButNotProcessed = new HashSet<String>();
    def allProcessedDefintionNames = new HashSet<String>();

    public SwaggerDefinitionsParser(SwaggerPropertiesParser swaggerPropertiesParser, DefinitionsWithManyVersions definitionsWithManyVersions) {
        this.swaggerPropertiesParser = swaggerPropertiesParser
        this.definitionsWithManyVersions = definitionsWithManyVersions
    }

    public Map<String, SwaggerDefinition> getDefinitionsFromJson(final JsonObject swaggerJson) {
        Map<String, SwaggerDefinition> swaggerDefinitions = [:]

        final JsonObject definitions = swaggerJson.get("definitions").getAsJsonObject();
        for (final Map.Entry<String, JsonElement> entry : definitions.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                final JsonObject definitionJsonObject = entry.getValue().getAsJsonObject();
                String name = entry.getKey()
                if (shouldProcessDefinition(name)) {
                    String versionlessName = name.replaceFirst(/V[0-9]+/, '')
                    List<SwaggerDefinitionProperty> swaggerDefinitionProperties = swaggerPropertiesParser.getPropertiesFromJson(name, definitionJsonObject)
                    if (swaggerDefinitionProperties.size() > 0) {
                        allProcessedDefintionNames.add(versionlessName)
                        def swaggerDefinition = new SwaggerDefinition();
                        swaggerDefinition.definitionName = versionlessName
                        swaggerDefinition.definitionProperties = swaggerDefinitionProperties
                        swaggerDefinition.definitionJsonObject = definitionJsonObject
                        swaggerDefinitions.put(versionlessName, swaggerDefinition);
                    }
                }
            }
        }
        return swaggerDefinitions
    }

    public Set<String> getUnknownDefinitionNames() {
        def unknownNames = definitionNamesReferencedButNotProcessed.findAll {
            !allProcessedDefintionNames.contains(it)
        }
        new HashSet<>(unknownNames)
    }

    public boolean shouldProcessDefinition(String name) {
        if (name.contains('Internal')) {
            return false
        }

        if (!definitionsWithManyVersions.shouldProcess(name)) {
            return false
        }

        //definitions with CONTAINER_START_MARKER and CONTAINER_END_MARKER reference a definition that should be defined elsewhere
        if (name.contains(CONTAINER_START_MARKER) && name.contains(CONTAINER_END_MARKER)) {
            int start = name.lastIndexOf(CONTAINER_START_MARKER) + 1
            int end = name.indexOf(CONTAINER_END_MARKER) - 1
            String nameThatShouldBeDefinedElsewhere = name[start..end]
            if (!definitionsWithManyVersions.shouldProcess(nameThatShouldBeDefinedElsewhere)) {
                return false
            }
            nameThatShouldBeDefinedElsewhere = nameThatShouldBeDefinedElsewhere.replaceFirst(/V[0-9]+/, '')
            if (!DEFINITIONS_TO_IGNORE_AND_CREATE_MANUALLY.contains(nameThatShouldBeDefinedElsewhere)) {
                //this definition won't be processed, but it creates a requirement on 'nameThatShouldBeDefinedElsewhere' to be processed
                definitionNamesReferencedButNotProcessed.add(nameThatShouldBeDefinedElsewhere)
            }
            return false
        } else if (DEFINITIONS_TO_IGNORE_AND_CREATE_MANUALLY.contains(name)) {
            return false
        }

        return true
    }

}
