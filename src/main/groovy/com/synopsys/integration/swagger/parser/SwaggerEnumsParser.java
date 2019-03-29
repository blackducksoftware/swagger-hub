/**
 * swagger-hub
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.swagger.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.synopsys.integration.swagger.model.SwaggerDefinitionProperty;

public class SwaggerEnumsParser {
    private final Map<String, List<String>> enumNameToValues = new HashMap<>();
    private final Map<String, String> valuesKeyToEnumName = new HashMap<>();

    public void populateEnumField(final SwaggerDefinitionProperty swaggerDefinitionProperty, final String definitionName, final String propertyName, final JsonObject propertyJsonObject) {
        final List<String> enumValues = getValues(propertyJsonObject);
        if (enumValues == null) {
            return;
        }
        Collections.sort(enumValues);
        String enumName = definitionName.replaceAll("(?i)view$", "");
        enumName = enumName.replaceFirst("V[0-9]+", "");
        enumName += StringUtils.capitalize(propertyName.replaceAll("(?i)type$", ""));
        if (enumName.toUpperCase().endsWith("TYPES")) {
            enumName = enumName.substring(0, enumName.length() - 1);
        } else {
            enumName += "Type";
        }
        enumName = removeDuplicateWords(enumName);
        enumNameToValues.put(enumName, enumValues);
        final String valuesKey = createValuesKey(enumValues);
        final String previousName = valuesKeyToEnumName.get(valuesKey);
        if (previousName != null) {
            if (enumName.length() < previousName.length()) {
                // enumName is the winner over previous
                valuesKeyToEnumName.put(valuesKey, enumName);
            }
        } else {
            // enumName is the winner by default
            valuesKeyToEnumName.put(valuesKey, enumName);
        }
        swaggerDefinitionProperty.enumType = enumName;
    }

    public Map<String, List<String>> getWinningNamesToValues() {
        final Map<String, List<String>> winningNamesToValues = new HashMap<>();
        enumNameToValues.forEach((key, value) -> {
            final String winningName = getWinningName(key);
            winningNamesToValues.put(winningName, value);
        });
        return winningNamesToValues;
    }

    public String getWinningName(final String enumName) {
        final List<String> enumValues = enumNameToValues.get(enumName);
        final String valuesKey = createValuesKey(enumValues);
        return valuesKeyToEnumName.get(valuesKey);
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

    private String createValuesKey(final List<String> enumValues) {
        return StringUtils.join(enumValues, "||");
    }

}
