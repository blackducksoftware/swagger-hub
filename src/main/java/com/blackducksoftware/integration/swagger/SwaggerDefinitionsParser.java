/*
 * Copyright (C) 2016 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.swagger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SwaggerDefinitionsParser {
    public List<SwaggerDefinition> getAllObjectDefinitions(JsonObject swaggerJson) {
        List<SwaggerDefinition> swaggerDefinitions = new ArrayList<>();
        JsonObject definitions = swaggerJson.get("definitions").getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : definitions.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                String name = entry.getKey();
                JsonObject definition = entry.getValue().getAsJsonObject();
                if (definition.has("properties")) {
                    Map<String, JsonObject> properties = new HashMap<>();
                    for (Map.Entry<String, JsonElement> property : definition.get("properties").getAsJsonObject().entrySet()) {
                        String propertyName = property.getKey();
                        properties.put(propertyName, property.getValue().getAsJsonObject());
                    }

                    SwaggerDefinition swaggerDefinition = new SwaggerDefinition(name, properties);
                    swaggerDefinitions.add(swaggerDefinition);
                }
            }
        }

        return swaggerDefinitions;
    }

}
