/**
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

public class SwaggerDefinitions {
    public static final String containerStartDefinitionMarker = '\u00ab'
    public static final String containerEndDefinitionMarker = '\u00bb'

    private static final List<String> knownDefinitionsToIgnore = [
        'boolean',
        'string',
        'DateTime',
        'VersionRiskProfileView',
        'string,RiskPriorityDistribution',
        'NameValuePairView',
        'ComponentHit'
    ]

    private final SwaggerProperties swaggerProperties

    def namesDefinedElsewhere = new HashSet<String>();
    def definitionNames = new HashSet<String>();

    public SwaggerDefinitions(SwaggerProperties swaggerProperties) {
        this.swaggerProperties = swaggerProperties
    }

    public Map<String, SwaggerDefinition> getDefinitionsFromJson(final JsonObject swaggerJson) {
        Map<String, SwaggerDefinition> swaggerDefinitions = [:]

        final JsonObject definitions = swaggerJson.get("definitions").getAsJsonObject();
        for (final Map.Entry<String, JsonElement> entry : definitions.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                final JsonObject definitionJsonObject = entry.getValue().getAsJsonObject();
                String name = entry.getKey()
                if (shouldProcessDefinition(name)) {
                    List<SwaggerDefinitionProperty> swaggerDefinitionProperties = swaggerProperties.getPropertiesFromJson(name, definitionJsonObject)
                    if (swaggerDefinitionProperties.size() > 0) {
                        definitionNames.add(name)
                        def swaggerDefinition = new SwaggerDefinition();
                        swaggerDefinition.definitionName = name
                        swaggerDefinition.definitionProperties = swaggerDefinitionProperties
                        swaggerDefinitions.put(name, swaggerDefinition);
                    }
                }
            }
        }
        return swaggerDefinitions
    }

    public Set<String> getUnknownDefinitionNames() {
        def unknownNames = namesDefinedElsewhere.findAll {
            !definitionNames.contains(it)
        }
        new HashSet<>(unknownNames)
    }

    public boolean shouldProcessDefinition(String name) {
        //definitions containing \u00ab and \u00bb () reference a definition that should be defined elsewhere
        if (name.contains(containerStartDefinitionMarker) || name.contains(containerEndDefinitionMarker)) {
            int start = name.lastIndexOf(containerStartDefinitionMarker) + 1
            int end = name.indexOf(containerEndDefinitionMarker) - 1
            String nameThatShouldBeDefinedElsewhere = name[start..end]
            if (!knownDefinitionsToIgnore.contains(nameThatShouldBeDefinedElsewhere)) {
                namesDefinedElsewhere.add(nameThatShouldBeDefinedElsewhere)
            }
            return false
        } else if (knownDefinitionsToIgnore.contains(name)) {
            return false
        }

        return true
    }

}
