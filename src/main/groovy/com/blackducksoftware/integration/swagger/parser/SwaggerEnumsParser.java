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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.swagger.model.SwaggerDefinitionProperty;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SwaggerEnumsParser {
    public final Map<String, List<String>> enumNameToValues = new HashMap<>();

    public void populateEnumField(final SwaggerDefinitionProperty swaggerDefinitionProperty, final String definitionName, final String propertyName, final JsonObject propertyJsonObject) {
        final List<String> enumValues = getValues(propertyJsonObject);
        if (enumValues == null) {
            return;
        }
        String enumName = definitionName.replaceAll("(?i)view$", "");
        enumName += StringUtils.capitalize(propertyName.replaceAll("(?i)type$", ""));
        enumName += "Enum";
        enumName = removeDuplicateWords(enumName);
        enumNameToValues.put(enumName, enumValues);
        swaggerDefinitionProperty.enumType = enumName;
    }

    private List<String> getValues(final JsonObject propertyJsonObject) {
        if (propertyJsonObject.has("enum")) {
            return getValuesFromArray(propertyJsonObject.getAsJsonArray("enum"));
        } else if (propertyJsonObject.has("items") && propertyJsonObject.get("items").isJsonObject() && propertyJsonObject.getAsJsonObject("items").has("enum")) {
            return getValuesFromArray(propertyJsonObject.getAsJsonObject("items").getAsJsonArray("enum"));
        } else {
            return null;
        }
    }

    private List<String> getValuesFromArray(final JsonArray enumValueArray) {
        final List<String> values = new ArrayList<>();
        for (final JsonElement enumValue : enumValueArray) {
            values.add(enumValue.getAsString());
        }
        return values;
    }

    private String removeDuplicateWords(final String name) {
        final List<String> repeatedWords = new ArrayList<>();

        final String[] words = StringUtils.splitByCharacterTypeCamelCase(name);
        if (words.length <= 1) {
            return name;
        }

        for (int wordIndex = 1; wordIndex < words.length; wordIndex++) {
            if (words[wordIndex].equalsIgnoreCase(words[wordIndex - 1])) {
                repeatedWords.add(words[wordIndex]);
            }
        }

        String nameWithoutDuplicates = name;
        for (final String repeated : repeatedWords) {
            nameWithoutDuplicates = nameWithoutDuplicates.replaceAll("(?i)(" + repeated + ")+", repeated);
        }

        return nameWithoutDuplicates;
    }

}
