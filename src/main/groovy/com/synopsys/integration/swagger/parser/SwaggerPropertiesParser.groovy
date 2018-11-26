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
package com.synopsys.integration.swagger.parser

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.synopsys.integration.swagger.model.SwaggerDefinitionProperty

class SwaggerPropertiesParser {
    private static final List<String> knownPropertyFields = ['format',
                                                             'type',
                                                             'description',
                                                             'readOnly',
                                                             'enum',
                                                             'items',
                                                             '$ref']

    private final SwaggerEnumsParser swaggerEnumsParser

    Map<String, Set<String>> propertyTypeToFormats = new HashMap<String, Set<String>>()
    Set<String> unknownPropertyFields = new HashSet<>()

    public SwaggerPropertiesParser(SwaggerEnumsParser swaggerEnumsParser) {
        this.swaggerEnumsParser = swaggerEnumsParser
    }

    public List<SwaggerDefinitionProperty> getPropertiesFromJson(String definitionName, final JsonObject definitionJsonObject) {
        List<SwaggerDefinitionProperty> swaggerDefinitionProperties = []
        if (definitionJsonObject.has("properties")) {
            JsonObject propertiesJsonObject = definitionJsonObject.get("properties").getAsJsonObject()
            for (final Map.Entry<String, JsonElement> property : propertiesJsonObject.entrySet()) {
                String propertyName = property.getKey();
                if (!property.getValue().isJsonObject()) {
                    throw new Exception("each property should be an object but ${propertyName} was not")
                }
                JsonObject propertyJsonObject = property.getValue().getAsJsonObject()
                SwaggerDefinitionProperty swaggerDefinitionProperty = new SwaggerDefinitionProperty()
                swaggerDefinitionProperty.name = propertyName
                populatePropertyFields(swaggerDefinitionProperty, propertyJsonObject)
                swaggerEnumsParser.populateEnumField(swaggerDefinitionProperty, definitionName, propertyName, propertyJsonObject)
                if (!propertyTypeToFormats.containsKey(swaggerDefinitionProperty.propertyType)) {
                    propertyTypeToFormats.put(swaggerDefinitionProperty.propertyType, new HashSet<>())
                }
                propertyTypeToFormats.get(swaggerDefinitionProperty.propertyType).add(swaggerDefinitionProperty.format)
                swaggerDefinitionProperties.add(swaggerDefinitionProperty)
            }
        }
        return swaggerDefinitionProperties
    }

    private void populatePropertyFields(SwaggerDefinitionProperty property, JsonObject propertyJsonObject) {
        for (final Map.Entry<String, JsonElement> field : propertyJsonObject.entrySet()) {
            if (!knownPropertyFields.contains(field.key)) {
                unknownPropertyFields.add(field.key)
            } else if ('format' == field.key) {
                property.format = field.value.asString
            } else if ('type' == field.key) {
                property.propertyType = field.value.asString
            } else if ('description' == field.key) {
                property.description = field.value.asString
            } else if ('readOnly' == field.key) {
                property.readOnly = field.value.asString
            } else if ('$ref' == field.key) {
                property.ref = field.value.asString
            }
        }
        property.propertyJsonObject = propertyJsonObject
    }
}