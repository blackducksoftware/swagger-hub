package com.blackducksoftware.integration.swagger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SwaggerEnumParser {
    public Map<String, List<String>> getEnumNameToValues(final List<SwaggerDefinition> allObjectDefinitions) {
        final Map<String, List<String>> enumNameToValues = new HashMap<>();

        for (final SwaggerDefinition swaggerDefinition : allObjectDefinitions) {
            for (final Map.Entry<String, JsonObject> propertyEntry : swaggerDefinition.getProperties().entrySet()) {
                final JsonObject property = propertyEntry.getValue();
                if (property.has("enum")) {
                    final JsonArray enumValueArray = property.get("enum").getAsJsonArray();
                    final List<String> values = new ArrayList<>();
                    for (final JsonElement enumValue : enumValueArray) {
                        values.add(enumValue.getAsString());
                    }

                    String enumName = swaggerDefinition.getName().replaceAll("(?i)view$", "");
                    enumName += StringUtils.capitalize(propertyEntry.getKey().replaceAll("(?i)type$", ""));
                    enumName += "Enum";
                    enumName = removeDuplicateWords(enumName);
                    enumNameToValues.put(enumName, values);
                }
            }
        }

        return enumNameToValues;
    }

    public String removeDuplicateWords(final String name) {
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
