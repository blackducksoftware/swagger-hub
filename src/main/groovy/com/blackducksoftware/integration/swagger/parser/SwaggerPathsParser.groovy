package com.blackducksoftware.integration.swagger.parser

import com.blackducksoftware.integration.swagger.model.ApiPath
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class SwaggerPathsParser {
    public List<ApiPath> getPathsToResponses(JsonObject swaggerJson) {
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
