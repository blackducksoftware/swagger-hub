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
package com.blackducksoftware.integration.swagger;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils

import com.blackducksoftware.integration.swagger.model.ApiPath
import com.blackducksoftware.integration.swagger.model.DefinitionLinkEntry
import com.blackducksoftware.integration.swagger.model.DefinitionLinks
import com.blackducksoftware.integration.swagger.model.SwaggerDefinition
import com.blackducksoftware.integration.swagger.parser.SwaggerDefinitionsParser
import com.blackducksoftware.integration.swagger.parser.SwaggerEnumsParser
import com.blackducksoftware.integration.swagger.parser.SwaggerPathsParser
import com.blackducksoftware.integration.swagger.parser.SwaggerPropertiesParser
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import freemarker.template.Configuration
import freemarker.template.Template

public class ModelCreator {
    public static final String BASE_DIRECTORY_ENVIRONMENT_VARIABLE = "SWAGGER_HUB_BASE_DIRECTORY";
    public static final String DEFAULT_BASE_DIRECTORY = "/tmp";

    public static final String DIRECTORY_PREFIX = "/com/blackducksoftware/integration/hub/api/generated";
    public static final String DISCOVERY_DIRECTORY = DIRECTORY_PREFIX + "/discovery";
    public static final String ENUM_DIRECTORY = DIRECTORY_PREFIX + "/enumeration";
    public static final String VIEW_DIRECTORY = DIRECTORY_PREFIX + "/view";
    public static final String RESPONSE_DIRECTORY = DIRECTORY_PREFIX + "/response";
    public static final String MODEL_DIRECTORY = DIRECTORY_PREFIX + "/model";

    public static final String API_CORE_PACKAGE_PREFIX = "com.blackducksoftware.integration.hub.api.core";
    public static final String GENERATED_PACKAGE_PREFIX = "com.blackducksoftware.integration.hub.api.generated";
    public static final String DISCOVERY_PACKAGE = GENERATED_PACKAGE_PREFIX + ".discovery";
    public static final String ENUM_PACKAGE = GENERATED_PACKAGE_PREFIX + ".enumeration";
    public static final String VIEW_PACKAGE = GENERATED_PACKAGE_PREFIX + ".view";
    public static final String RESPONSE_PACKAGE = GENERATED_PACKAGE_PREFIX + ".response";
    public static final String MODEL_PACKAGE = GENERATED_PACKAGE_PREFIX + ".model";

    public static void main(final String[] args) throws Exception {
        final File jsonFile = new File(ModelCreator.class.getClassLoader().getResource("api-docs_4.4.0.json").toURI());
        final FileInputStream jsonFileInputStream = new FileInputStream(jsonFile);
        final InputStreamReader jsonInputStreamReader = new InputStreamReader(jsonFileInputStream, StandardCharsets.UTF_8);

        final JsonParser jsonParser = new JsonParser();
        final JsonObject swaggerJson = jsonParser.parse(jsonInputStreamReader).getAsJsonObject();

        final File definitionsThatAreHubViews = new File(ModelCreator.class.getClassLoader().getResource("definitions_that_are_hub_views.txt").toURI());
        List<String> definitionThatAreHubViewsLines = definitionsThatAreHubViews.readLines()
        Set<String> definitionNamesToExtendHubView = new HashSet<>(definitionThatAreHubViewsLines);

        final File definitionsThatAreHubResponses = new File(ModelCreator.class.getClassLoader().getResource("definitions_that_are_hub_responses.txt").toURI());
        List<String> definitionThatAreHubResponsesLines = definitionsThatAreHubResponses.readLines()
        Set<String> definitionNamesToExtendHubResponse = new HashSet<>(definitionThatAreHubResponsesLines);

        final File definitionsWithLinksFile = new File(ModelCreator.class.getClassLoader().getResource("definitions_with_links.txt").toURI());
        List<DefinitionLinkEntry> linkEntries = []
        definitionsWithLinksFile.eachLine { line ->
            if (line && !line.startsWith('#')) {
                String[] pieces = line.split(',')
                def linkEntry = new DefinitionLinkEntry()
                linkEntry.definitionName = pieces[0]
                linkEntry.link = pieces[1]
                if (pieces.length == 4) {
                    linkEntry.canHaveManyResults = Boolean.valueOf(pieces[2])
                    linkEntry.resultClass = pieces[3]
                }
                linkEntries.add(linkEntry)
            }
        }

        final SwaggerEnumsParser swaggerEnumsParser = new SwaggerEnumsParser()
        final SwaggerPropertiesParser swaggerPropertiesParser = new SwaggerPropertiesParser(swaggerEnumsParser)
        final SwaggerDefinitionsParser swaggerDefinitionsParser = new SwaggerDefinitionsParser(swaggerPropertiesParser);
        final Map<String, SwaggerDefinition> allObjectDefinitions = swaggerDefinitionsParser.getDefinitionsFromJson(swaggerJson)
        println allObjectDefinitions.keySet().size()
        final DefinitionLinks definitionLinks = new DefinitionLinks(linkEntries, allObjectDefinitions.keySet(), definitionNamesToExtendHubView, definitionNamesToExtendHubResponse, swaggerEnumsParser.winningNamesToValues.keySet())

        final SwaggerPathsParser swaggerPathsParser = new SwaggerPathsParser();
        final List<ApiPath> apiPaths = swaggerPathsParser.getPathsToResponses(swaggerJson);

        logPossibleErrors(swaggerDefinitionsParser, swaggerPropertiesParser, allObjectDefinitions);

        Set<String> possibleReferencesForProperties = new HashSet<>();
        possibleReferencesForProperties.addAll(allObjectDefinitions.keySet());
        possibleReferencesForProperties.addAll(SwaggerDefinitionsParser.DEFINITIONS_TO_IGNORE_AND_CREATE_MANUALLY);

        final Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setClassForTemplateLoading(ModelCreator.class, "/");
        configuration.setDefaultEncoding("UTF-8");
        Template discoveryTemplate = configuration.getTemplate("hubDiscovery.ftl");
        Template enumTemplate = configuration.getTemplate("hubEnum.ftl");
        Template viewTemplate = configuration.getTemplate("hubView.ftl");

        File discoveryBaseDirectory = new File(getBaseDirectory(), ModelCreator.DISCOVERY_DIRECTORY);
        File enumBaseDirectory = new File(getBaseDirectory(), ModelCreator.ENUM_DIRECTORY);
        File viewBaseDirectory = new File(getBaseDirectory(), ModelCreator.VIEW_DIRECTORY);
        File responseBaseDirectory = new File(getBaseDirectory(), ModelCreator.RESPONSE_DIRECTORY);
        File modelBaseDirectory = new File(getBaseDirectory(), ModelCreator.MODEL_DIRECTORY);
        discoveryBaseDirectory.mkdirs();
        enumBaseDirectory.mkdirs();
        viewBaseDirectory.mkdirs();
        responseBaseDirectory.mkdirs();
        modelBaseDirectory.mkdirs();
        createDiscoveryFile(discoveryBaseDirectory, discoveryTemplate, apiPaths, definitionLinks)
        createEnumFiles(enumBaseDirectory, enumTemplate, swaggerEnumsParser.winningNamesToValues);
        createViewFiles(getBaseDirectory(), viewTemplate, new ArrayList<>(allObjectDefinitions.values()), possibleReferencesForProperties, definitionNamesToExtendHubView, definitionNamesToExtendHubResponse, definitionLinks, swaggerEnumsParser);
    }

    public static File getBaseDirectory() {
        String baseDirectory = System.getenv(BASE_DIRECTORY_ENVIRONMENT_VARIABLE);
        if (!baseDirectory) {
            baseDirectory = DEFAULT_BASE_DIRECTORY;
        }
        return new File(baseDirectory);
    }

    public static void createDiscoveryFile(File baseDirectory, Template template, List<ApiPath> apiPaths, DefinitionLinks definitionLinks) {
        File discoveryFile = new File(baseDirectory, "ApiDiscovery.java");

        final Map<String, Object> model = new HashMap<>();
        model.put("discoveryPackage", ModelCreator.DISCOVERY_PACKAGE)

        Set imports = new HashSet<>()
        List links = new ArrayList<>()
        model.put("links", links)

        apiPaths.each {
            String importPackage = definitionLinks.getFullyQualifiedClassName(it.resultClass);
            imports.add(importPackage);
            imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "LinkResponse");
            imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "LinkSingleResponse");
            imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "LinkMultipleResponses");

            Map<String, Object> linkModel = new HashMap<>();
            linkModel.put("label", it.path);
            linkModel.put("javaConstant", it.javaConstant);
            linkModel.put("resultClass", it.resultClass);
            if (it.hasManyResults) {
                linkModel.put("hasMultipleResults", true);
                linkModel.put("linkType", "LinkMultipleResponses<${it.resultClass}>");
            } else {
                linkModel.put("linkType", "LinkSingleResponse<${it.resultClass}>");
            }
            links.add(linkModel);
        }

        List sortedImports = new ArrayList<>(imports);
        Collections.sort(sortedImports);
        model.put("imports", sortedImports)

        final FileWriter fileWriter = new FileWriter(discoveryFile);
        template.process(model, fileWriter);
    }

    public static void createEnumFiles(File baseDirectory, Template template, Map<String, List<String>> enumNameToValues) {
        enumNameToValues.each { k,v ->
            File enumFile = new File(baseDirectory, k + ".java");

            final Map<String, Object> model = new HashMap<>();
            model.put("enumPackage", ModelCreator.ENUM_PACKAGE)
            model.put("enumClassName", k);
            model.put("enumValues", v);

            final FileWriter fileWriter = new FileWriter(enumFile);
            template.process(model, fileWriter);
        }
    }

    public static void createViewFiles(File baseDirectory, Template template, List<SwaggerDefinition> swaggerDefinitions, Set<String> possibleReferencesForProperties, final Set<String> definitionNamesToExtendHubView, final Set<String> definitionNamesToExtendHubResponse, DefinitionLinks definitionLinks, SwaggerEnumsParser swaggerEnumsParser) {
        swaggerDefinitions.each {
            try {
                File viewFile = baseDirectory
                final Map<String, Object> model = new HashMap<>();
                model.put("className", it.definitionName);

                Set imports = new HashSet<>()

                if (definitionNamesToExtendHubView.contains(it.definitionName)) {
                    model.put("viewPackage", ModelCreator.VIEW_PACKAGE)
                    model.put("baseClass", "HubView");
                    imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "HubView");
                    viewFile = new File(viewFile, ModelCreator.VIEW_DIRECTORY);
                } else if (definitionNamesToExtendHubResponse.contains(it.definitionName)) {
                    model.put("viewPackage", ModelCreator.RESPONSE_PACKAGE)
                    model.put("baseClass", "HubResponse");
                    imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "HubResponse");
                    viewFile = new File(viewFile, ModelCreator.RESPONSE_DIRECTORY);
                } else {
                    model.put("viewPackage", ModelCreator.MODEL_PACKAGE)
                    model.put("baseClass", "HubModel");
                    imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "HubModel");
                    viewFile = new File(viewFile, ModelCreator.MODEL_DIRECTORY);
                }
                viewFile = new File(viewFile, it.definitionName + ".java");

                Map<String, String> definitionLinksToConstants = definitionLinks.getLinksToJavaConstants(it.definitionName)
                if (definitionLinksToConstants != null && !definitionLinksToConstants.empty) {
                    model.put("hasLinks", true)
                    List links = new ArrayList<>()
                    model.put("links", links)
                    definitionLinksToConstants.each { link, constant ->
                        Map<String, Object> linkModel = new HashMap<>();
                        linkModel.put("label", link);
                        linkModel.put("javaConstant", constant);

                        String resultClass = definitionLinks.getResultClass(it.definitionName, link)
                        if (StringUtils.isNotBlank(resultClass)) {
                            model.put('hasLinksWithResults', true);
                            String importPackage = definitionLinks.getFullyQualifiedClassName(resultClass);
                            if (StringUtils.isNotBlank(importPackage)) {
                                imports.add(importPackage);
                            }
                            imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "LinkResponse");
                            linkModel.put("resultClass", resultClass);
                            if ("String".equals(resultClass)) {
                                imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "LinkStringResponse");
                                linkModel.put("linkType", "LinkStringResponse");
                            } else if (definitionLinks.canHaveManyResults(it.definitionName, link)) {
                                imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "LinkMultipleResponses");
                                linkModel.put("hasMultipleResults", true);
                                linkModel.put("linkType", "LinkMultipleResponses<${resultClass}>");
                            } else {
                                imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "LinkSingleResponse");
                                linkModel.put("linkType", "LinkSingleResponse<${resultClass}>");
                            }
                        }
                        links.add(linkModel)
                    }
                }

                List<Map<String, Object>> fields = new ArrayList<>();
                model.put("classFields", fields);

                it.definitionProperties.each { property ->
                    Map<String, Object> propertyModel = new HashMap<>();
                    propertyModel.put("name", property.name);
                    String propertyType = property.getFullyQualifiedClassName(imports, definitionLinks, swaggerEnumsParser, possibleReferencesForProperties);
                    String importPackage = definitionLinks.getFullyQualifiedClassName(propertyType)
                    if (StringUtils.isNotBlank(importPackage)) {
                        imports.add(importPackage);
                    }
                    propertyModel.put("type", propertyType);
                    fields.add(propertyModel)
                }

                List sortedImports = new ArrayList<>(imports);
                Collections.sort(sortedImports);
                model.put("imports", sortedImports)

                final FileWriter fileWriter = new FileWriter(viewFile);
                template.process(model, fileWriter);
            } catch (Exception e) {
                throw new Exception("Exception caught processing ${it.definitionName}: " + e.getMessage(), e);
            }
        }
    }

    public static logPossibleErrors(SwaggerDefinitionsParser swaggerDefinitionsParser, SwaggerPropertiesParser swaggerPropertiesParser, Map<String, SwaggerDefinition> allObjectDefinitions) {
        println "Unknown definitions (possible problems) (${swaggerDefinitionsParser.unknownDefinitionNames.size()}):"
        swaggerDefinitionsParser.unknownDefinitionNames.toSorted { a, b ->
            a <=> b
        }.each { println it }
        println '---------------------------------------'

        println "Unknown property fields (possible problems) (${swaggerPropertiesParser.unknownPropertyFields.size()}):"
        swaggerPropertiesParser.unknownPropertyFields.toSorted { a, b ->
            a <=> b
        }.each { println it }
        println '---------------------------------------'

        println "Found definitions (${swaggerDefinitionsParser.allProcessedDefintionNames.size()}):"
        swaggerDefinitionsParser.allProcessedDefintionNames.toSorted { a, b ->
            a <=> b
        }.each { println it }
        println '---------------------------------------'

        println "Found types/formats:"
        swaggerPropertiesParser.propertyTypeToFormats.each { k, v ->
            println "${k}: formats(${v.join(', ')})"
        }
        println '---------------------------------------'

        allObjectDefinitions.each { k,v -> println v }
    }
}