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
package com.synopsys.integration.swagger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.synopsys.integration.swagger.creator.ComponentCreator;
import com.synopsys.integration.swagger.model.ApiPath;
import com.synopsys.integration.swagger.model.DefinitionLinkEntry;
import com.synopsys.integration.swagger.model.DefinitionLinks;
import com.synopsys.integration.swagger.model.SwaggerDefinition;
import com.synopsys.integration.swagger.parser.SwaggerDefinitionsParser;
import com.synopsys.integration.swagger.parser.SwaggerEnumsParser;
import com.synopsys.integration.swagger.parser.SwaggerPathsParser;
import com.synopsys.integration.swagger.parser.SwaggerPropertiesParser;
import com.synopsys.integration.swagger.util.ResourceUtil;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class ModelCreator {
    // Resources
    public static final String API_DOCS = "api-docs/api-docs_5.0.0_manually_edited.json";

    public static final String OVERRIDES_RESOURCE_PATH = "overrides/";
    public static final String API_PATH_IGNORE = OVERRIDES_RESOURCE_PATH + "api_path_ignore.txt";
    public static final String API_PATH_OVERRIDES = OVERRIDES_RESOURCE_PATH + "api_path_overrides.txt";
    public static final String RESPONSE_DEFINITIONS = OVERRIDES_RESOURCE_PATH + "definitions_that_are_black_duck_responses.txt";
    public static final String VIEW_DEFINITIONS = OVERRIDES_RESOURCE_PATH + "definitions_that_are_black_duck_views.txt";
    public static final String DEFINITIONS_WITH_LINKS = OVERRIDES_RESOURCE_PATH + "definitions_with_links.txt";

    public static final String TEMPLATES_RESOURCE_PATH = "templates/";
    public static final String BLACK_DUCK_DISCOVERY = TEMPLATES_RESOURCE_PATH + "blackDuckDiscovery.ftl";
    public static final String BLACK_DUCK_ENUM = TEMPLATES_RESOURCE_PATH + "blackDuckEnum.ftl";
    public static final String BLACK_DUCK_VIEW = TEMPLATES_RESOURCE_PATH + "blackDuckView.ftl";

    // Other stuff
    public static final String BASE_DIRECTORY_ENVIRONMENT_VARIABLE = "SWAGGER_BLACK_DUCK_BASE_DIRECTORY";
    public static final String DEFAULT_BASE_DIRECTORY = "/tmp";

    public static final String DIRECTORY_PREFIX = "/com/synopsys/integration/blackduck/api/generated";
    public static final String DISCOVERY_DIRECTORY = DIRECTORY_PREFIX + "/discovery";
    public static final String ENUM_DIRECTORY = DIRECTORY_PREFIX + "/enumeration";
    public static final String VIEW_DIRECTORY = DIRECTORY_PREFIX + "/view";
    public static final String RESPONSE_DIRECTORY = DIRECTORY_PREFIX + "/response";
    public static final String COMPONENT_DIRECTORY = DIRECTORY_PREFIX + "/component";

    public static final String API_CORE_PACKAGE_PREFIX = "com.synopsys.integration.blackduck.api.core";
    public static final String GENERATED_PACKAGE_PREFIX = "com.synopsys.integration.blackduck.api.generated";
    public static final String DISCOVERY_PACKAGE = GENERATED_PACKAGE_PREFIX + ".discovery";
    public static final String ENUM_PACKAGE = GENERATED_PACKAGE_PREFIX + ".enumeration";
    public static final String VIEW_PACKAGE_SUFFIX = ".view";
    public static final String RESPONSE_PACKAGE_SUFFIX = ".response";
    public static final String COMPONENT_PACKAGE_SUFFIX = ".component";
    public static final String VIEW_PACKAGE = GENERATED_PACKAGE_PREFIX + VIEW_PACKAGE_SUFFIX;
    public static final String RESPONSE_PACKAGE = GENERATED_PACKAGE_PREFIX + RESPONSE_PACKAGE_SUFFIX;
    public static final String COMPONENT_PACKAGE = GENERATED_PACKAGE_PREFIX + COMPONENT_PACKAGE_SUFFIX;

    public static void main(final String[] args) throws Exception {
        final ModelCreator modelCreator = new ModelCreator(new ResourceUtil());
        modelCreator.generateModel();
    }

    private final ResourceUtil resourceUtil;

    public ModelCreator(final ResourceUtil resourceUtil) {
        this.resourceUtil = resourceUtil;
    }

    private boolean isOverrideLineValid(final String line) {
        return StringUtils.isNotBlank(line) && !line.startsWith("#");
    }

    private String[] mapLineToPieces(final String line) {
        return line.trim().split(",");
    }

    public void generateModel() throws IOException, TemplateException {
        final InputStream swaggerJsonInputStream = resourceUtil.getResourceAsStream(API_DOCS);
        final Reader jsonInputStreamReader = new InputStreamReader(swaggerJsonInputStream, ResourceUtil.DEFAULT_RESOURCE_ENCODING);
        final JsonParser jsonParser = new JsonParser();
        final JsonObject swaggerJson = jsonParser.parse(jsonInputStreamReader).getAsJsonObject();

        final List<String> definitionThatAreBlackDuckViewsLines = resourceUtil.getLinesFromResource(VIEW_DEFINITIONS);
        final Set<String> definitionNamesToExtendBlackDuckView = new HashSet<>(definitionThatAreBlackDuckViewsLines);

        final List<String> definitionThatAreBlackDuckResponsesLines = resourceUtil.getLinesFromResource(RESPONSE_DEFINITIONS);
        final Set<String> definitionNamesToExtendBlackDuckResponse = new HashSet<>(definitionThatAreBlackDuckResponsesLines);

        final List<DefinitionLinkEntry> linkEntries = resourceUtil.getLinesFromResource(DEFINITIONS_WITH_LINKS).stream()
                                                          .filter(this::isOverrideLineValid)
                                                          .map(this::mapLineToPieces)
                                                          .map(DefinitionLinkEntry::fromArray)
                                                          .collect(Collectors.toList());

        final Map<String, String> overrideEntries = resourceUtil.getLinesFromResource(API_PATH_OVERRIDES).stream()
                                                        .filter(this::isOverrideLineValid)
                                                        .map(this::mapLineToPieces)
                                                        .collect(Collectors.toMap((pieces) -> pieces[0], (pieces) -> pieces[1]));

        final Set<String> apiPathsToIgnore = resourceUtil.getLinesFromResource(API_PATH_IGNORE).stream()
                                                 .filter(this::isOverrideLineValid)
                                                 .collect(Collectors.toSet());

        final SwaggerEnumsParser swaggerEnumsParser = new SwaggerEnumsParser();
        final SwaggerPropertiesParser swaggerPropertiesParser = new SwaggerPropertiesParser(swaggerEnumsParser);
        final SwaggerDefinitionsParser swaggerDefinitionsParser = new SwaggerDefinitionsParser(swaggerPropertiesParser);
        final Map<String, SwaggerDefinition> allObjectDefinitions = swaggerDefinitionsParser.getDefinitionsFromJson(swaggerJson);
        System.out.println(allObjectDefinitions.size());
        final DefinitionLinks definitionLinks = new DefinitionLinks(linkEntries, allObjectDefinitions.keySet(), definitionNamesToExtendBlackDuckView, definitionNamesToExtendBlackDuckResponse,
            swaggerEnumsParser.getWinningNamesToValues().keySet());

        final SwaggerPathsParser swaggerPathsParser = new SwaggerPathsParser();
        final List<ApiPath> apiPaths = swaggerPathsParser.getPathsToResponses(swaggerJson, apiPathsToIgnore, overrideEntries);

        logPossibleErrors(swaggerDefinitionsParser, swaggerPropertiesParser, allObjectDefinitions);

        final Set<String> possibleReferencesForProperties = new HashSet<>();
        possibleReferencesForProperties.addAll(allObjectDefinitions.keySet());
        possibleReferencesForProperties.addAll(SwaggerDefinitionsParser.DEFINITIONS_TO_IGNORE_AND_CREATE_MANUALLY);

        final Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setClassForTemplateLoading(ModelCreator.class, "/");
        configuration.setDefaultEncoding(ResourceUtil.DEFAULT_RESOURCE_ENCODING.toString());
        final Template discoveryTemplate = configuration.getTemplate(BLACK_DUCK_DISCOVERY);
        final Template enumTemplate = configuration.getTemplate(BLACK_DUCK_ENUM);
        final Template viewTemplate = configuration.getTemplate(BLACK_DUCK_VIEW);

        final File discoveryBaseDirectory = new File(getBaseDirectory(), ModelCreator.DISCOVERY_DIRECTORY);
        final File enumBaseDirectory = new File(getBaseDirectory(), ModelCreator.ENUM_DIRECTORY);
        final File viewBaseDirectory = new File(getBaseDirectory(), ModelCreator.VIEW_DIRECTORY);
        final File responseBaseDirectory = new File(getBaseDirectory(), ModelCreator.RESPONSE_DIRECTORY);
        final File modelBaseDirectory = new File(getBaseDirectory(), ModelCreator.COMPONENT_DIRECTORY);
        discoveryBaseDirectory.mkdirs();
        enumBaseDirectory.mkdirs();
        viewBaseDirectory.mkdirs();
        responseBaseDirectory.mkdirs();
        modelBaseDirectory.mkdirs();
        createDiscoveryFile(discoveryBaseDirectory, discoveryTemplate, apiPaths, definitionLinks);
        createEnumFiles(enumBaseDirectory, enumTemplate, swaggerEnumsParser.getWinningNamesToValues());

        final ComponentCreator componentCreator = new ComponentCreator();
        componentCreator
            .createViewFiles(getBaseDirectory(), viewTemplate, new ArrayList<>(allObjectDefinitions.values()), possibleReferencesForProperties, definitionNamesToExtendBlackDuckView, definitionNamesToExtendBlackDuckResponse, definitionLinks,
                swaggerEnumsParser);
    }

    public static File getBaseDirectory() {
        String baseDirectory = System.getenv(BASE_DIRECTORY_ENVIRONMENT_VARIABLE);
        if (StringUtils.isBlank(baseDirectory)) {
            baseDirectory = DEFAULT_BASE_DIRECTORY;
        }
        return new File(baseDirectory);
    }

    public static void createDiscoveryFile(final File baseDirectory, final Template template, final List<ApiPath> apiPaths, final DefinitionLinks definitionLinks) throws IOException, TemplateException {
        final File discoveryFile = new File(baseDirectory, "ApiDiscovery.java");

        final Map<String, Object> model = new HashMap<>();
        model.put("discoveryPackage", ModelCreator.DISCOVERY_PACKAGE);

        final Set<String> imports = new HashSet<>();
        final List<Map<String, Object>> links = new ArrayList<>();
        model.put("links", links);

        for (final ApiPath apiPath : apiPaths) {
            final String importPackage = definitionLinks.getFullyQualifiedClassName(apiPath.resultClass);
            if (null == importPackage) {
                System.out.println("couldn't find package for: " + apiPath.resultClass);
            }
            imports.add(importPackage);
            imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "LinkResponse");
            imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "BlackDuckPath");
            imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "BlackDuckPathSingleResponse");
            imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "BlackDuckPathMultipleResponses");

            final Map<String, Object> linkModel = new HashMap<>();
            linkModel.put("label", apiPath.path);
            linkModel.put("javaConstant", apiPath.getJavaConstant());
            linkModel.put("resultClass", apiPath.resultClass);
            if (apiPath.hasManyResults) {
                linkModel.put("hasMultipleResults", true);
                linkModel.put("linkType", String.format("BlackDuckPathMultipleResponses<%s>", apiPath.resultClass));
            } else {
                linkModel.put("linkType", String.format("BlackDuckPathSingleResponse<%s>", apiPath.resultClass));
            }
            links.add(linkModel);
        }

        final List<String> sortedImports = new ArrayList<>(imports);
        Collections.sort(sortedImports);
        model.put("imports", sortedImports);

        final FileWriter fileWriter = new FileWriter(discoveryFile);
        template.process(model, fileWriter);
    }

    public static void createEnumFiles(final File baseDirectory, final Template template, final Map<String, List<String>> enumNameToValues) throws IOException, TemplateException {
        for (final Map.Entry<String, List<String>> enumEntry : enumNameToValues.entrySet()) {
            final String enumName = enumEntry.getKey();
            final List<String> enumValues = enumEntry.getValue();

            final File enumFile = new File(baseDirectory, enumName + ".java");

            final Map<String, Object> model = new HashMap<>();
            model.put("enumPackage", ModelCreator.ENUM_PACKAGE);
            model.put("enumClassName", enumName);
            model.put("enumValues", enumValues);

            final FileWriter fileWriter = new FileWriter(enumFile);
            template.process(model, fileWriter);
        }
    }

    private void logPossibleErrors(final SwaggerDefinitionsParser swaggerDefinitionsParser, final SwaggerPropertiesParser swaggerPropertiesParser, final Map<String, SwaggerDefinition> allObjectDefinitions) {
        System.out.println(String.format("Unknown definitions (possible problems) (%d):", swaggerDefinitionsParser.getUnknownDefinitionNames().size()));
        swaggerDefinitionsParser.getUnknownDefinitionNames().stream()
            .sorted(Comparator.naturalOrder())
            .forEach(System.out::println);

        System.out.println("---------------------------------------");

        System.out.println(String.format("Unknown property fields (possible problems) (%d):", swaggerPropertiesParser.getUnknownPropertyFields().size()));
        swaggerPropertiesParser.getUnknownPropertyFields().stream()
            .sorted(Comparator.naturalOrder())
            .forEach(System.out::println);
        System.out.println("---------------------------------------");

        System.out.println(String.format("Found definitions (%d):", swaggerDefinitionsParser.getAllProcessedDefintionNames().size()));
        swaggerDefinitionsParser.getAllProcessedDefintionNames().stream()
            .sorted(Comparator.naturalOrder())
            .forEach(System.out::println);
        System.out.println("---------------------------------------");

        System.out.println("Found types/formats:");
        swaggerPropertiesParser.getPropertyTypeToFormats()
            .forEach((key, value) -> System.out.println(String.format("%s: formats(%s)", key, StringUtils.join(value, ", "))));
        System.out.println("---------------------------------------");

        allObjectDefinitions.forEach((key, value) -> System.out.println(value));
    }
}