package com.blackducksoftware.integration.swagger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.swagger.freemarker.EnumCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RestServiceClassGenerator {
    public static void main(final String[] args) throws IOException, URISyntaxException {
        File apiJsonFile = new File(EnumCreator.class.getClassLoader().getResource("api-docs_3.3.1.json").toURI());
        final FileInputStream jsonFile = new FileInputStream(apiJsonFile);
        final String json = IOUtils.toString(jsonFile, "UTF-8");

        final JsonParser jsonParser = new JsonParser();
        final JsonObject swaggerJson = jsonParser.parse(json).getAsJsonObject();

        final RestServiceClassGenerator restServiceClassGenerator = new RestServiceClassGenerator();
        final List<String> classNames = restServiceClassGenerator.findClassNames(swaggerJson);
        final Map<String, List<String>> classNameToMethodNames = restServiceClassGenerator
                .findClassNameToMethodNames(swaggerJson);

        System.out.println(String.format("%d declared class names", classNames.size()));
        System.out.println(String.format("%d class names with methods", classNameToMethodNames.keySet().size()));

        for (final String className : classNames) {
            System.out.println(className);
            if (classNameToMethodNames.containsKey(className)) {
                final List<String> methodNames = classNameToMethodNames.get(className);
                for (final String methodName : methodNames) {
                    System.out.println("\t" + methodName);
                }
            }
        }
    }

    public List<String> findClassNames(final JsonObject swaggerJson) {
        final List<String> classNames = new ArrayList<>();

        final JsonArray tags = swaggerJson.get("tags").getAsJsonArray();
        for (final JsonElement tagElement : tags) {
            final String restServerName = tagElement.getAsJsonObject().get("name").getAsString();
            final String className = convertRestServerNameToClassName(restServerName);
            classNames.add(className);
        }

        Collections.sort(classNames);
        return classNames;
    }

    public Map<String, List<String>> findClassNameToMethodNames(final JsonObject swaggerJson) {
        final Map<String, List<String>> classNameToMethodNames = new HashMap<>();

        final JsonObject paths = swaggerJson.get("paths").getAsJsonObject();
        for (final Map.Entry<String, JsonElement> entry : paths.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                final String hubUrl = entry.getKey();
                final JsonObject hubUrlEndpoint = entry.getValue().getAsJsonObject();

                final Set<Map.Entry<String, JsonElement>> httpMethods = hubUrlEndpoint.entrySet();
                for (final Map.Entry<String, JsonElement> httpMethodEntry : httpMethods) {
                    final JsonObject httpMethod = httpMethodEntry.getValue().getAsJsonObject();
                    final JsonArray tags = httpMethod.get("tags").getAsJsonArray();
                    if (tags.size() != 1) {
                        throw new RuntimeException(String.format("Only expected one tag for httpMethod %s in %s ",
                                httpMethodEntry.getKey(), hubUrl));
                    }

                    final String restServerName = tags.get(0).getAsString();
                    final String className = convertRestServerNameToClassName(restServerName);
                    final String methodName = httpMethod.get("summary").getAsString();

                    if (!classNameToMethodNames.containsKey(className)) {
                        classNameToMethodNames.put(className, new ArrayList<String>());
                    }
                    classNameToMethodNames.get(className).add(hubUrl + " - " + methodName);
                }
            }
        }

        for (final List<String> methodNames : classNameToMethodNames.values()) {
            Collections.sort(methodNames);
        }
        return classNameToMethodNames;
    }

    public Map<String, List<String>> findEnumNameToEnumValues() {
        return null;
    }

    private String convertRestServerNameToClassName(final String restServerName) {
        final String[] classNamePieces = restServerName.replace("-rest-server", "").split("\\-");
        final StringBuilder classNameBuilder = new StringBuilder();
        for (final String piece : classNamePieces) {
            classNameBuilder.append(StringUtils.capitalize(piece));
        }
        classNameBuilder.append("RestService");
        return classNameBuilder.toString();
    }

}
