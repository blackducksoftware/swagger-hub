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
package com.blackducksoftware.integration.swagger.parser;

import com.blackducksoftware.integration.swagger.model.SwaggerDefinition
import com.blackducksoftware.integration.swagger.model.SwaggerDefinitionProperty
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SwaggerDefinitionsParser {
    public static final String CONTAINER_START_MARKER = '\u00ab'
    public static final String CONTAINER_END_MARKER = '\u00bb'
    public static final List<String> DEFINITIONS_TO_IGNORE_AND_CREATE_MANUALLY = [
        'boolean',
        'string',
        'DateTime',
        'string,RiskPriorityDistribution',
        'Request to create a custom license',
        'vulnerability remediation report request'
    ]

    private final SwaggerPropertiesParser swaggerPropertiesParser

    def definitionNamesReferencedButNotProcessed = new HashSet<String>();
    def allProcessedDefintionNames = new HashSet<String>();

    public SwaggerDefinitionsParser(SwaggerPropertiesParser swaggerPropertiesParser) {
        this.swaggerPropertiesParser = swaggerPropertiesParser
    }

    public Map<String, SwaggerDefinition> getDefinitionsFromJson(final JsonObject swaggerJson) {
        Map<String, SwaggerDefinition> swaggerDefinitions = [:]

        final JsonObject definitions = swaggerJson.get("definitions").getAsJsonObject();
        for (final Map.Entry<String, JsonElement> entry : definitions.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                final JsonObject definitionJsonObject = entry.getValue().getAsJsonObject();
                String name = entry.getKey()
                if (shouldProcessDefinition(name)) {
                    List<SwaggerDefinitionProperty> swaggerDefinitionProperties = swaggerPropertiesParser.getPropertiesFromJson(name, definitionJsonObject)
                    if (swaggerDefinitionProperties.size() > 0) {
                        allProcessedDefintionNames.add(name)
                        def swaggerDefinition = new SwaggerDefinition();
                        swaggerDefinition.definitionName = name
                        swaggerDefinition.definitionProperties = swaggerDefinitionProperties
                        swaggerDefinition.definitionJsonObject = definitionJsonObject
                        swaggerDefinitions.put(name, swaggerDefinition);
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
        //definitions with CONTAINER_START_MARKER and CONTAINER_END_MARKER reference a definition that should be defined elsewhere
        if (name.contains(CONTAINER_START_MARKER) && name.contains(CONTAINER_END_MARKER)) {
            int start = name.lastIndexOf(CONTAINER_START_MARKER) + 1
            int end = name.indexOf(CONTAINER_END_MARKER) - 1
            String nameThatShouldBeDefinedElsewhere = name[start..end]
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
