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
    public Map<String, List<String>> getEnumNameToValues(List<SwaggerDefinition> allObjectDefinitions) {
        Map<String, List<String>> enumNameToValues = new HashMap<>();

        for (SwaggerDefinition swaggerDefinition : allObjectDefinitions) {
            for (Map.Entry<String, JsonObject> propertyEntry : swaggerDefinition.getProperties().entrySet()) {
                JsonObject property = propertyEntry.getValue();
                if (property.has("enum")) {
                    JsonArray enumValueArray = property.get("enum").getAsJsonArray();
                    List<String> values = new ArrayList<>();
                    for (JsonElement enumValue : enumValueArray) {
                        values.add(enumValue.getAsString());
                    }

                    String enumName = swaggerDefinition.getName().replaceAll("(?i)view$", "");
                    enumName += StringUtils.capitalize(propertyEntry.getKey().replaceAll("(?i)type$", ""));
                    enumName += "Enum";
                    enumNameToValues.put(enumName, values);
                }
            }
        }

        return enumNameToValues;
    }

}
