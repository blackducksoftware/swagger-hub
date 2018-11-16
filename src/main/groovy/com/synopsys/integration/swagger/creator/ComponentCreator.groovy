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
package com.synopsys.integration.swagger.creator

import com.synopsys.integration.swagger.ModelCreator
import com.synopsys.integration.swagger.model.DefinitionLinks
import com.synopsys.integration.swagger.model.FullyQualifiedClassName
import com.synopsys.integration.swagger.model.SwaggerDefinition
import com.synopsys.integration.swagger.parser.SwaggerEnumsParser
import freemarker.template.Template
import org.apache.commons.lang3.StringUtils

public class ComponentCreator {
    public void createViewFiles(final File baseDirectory,
                                final Template template,
                                final List<SwaggerDefinition> swaggerDefinitions,
                                final Set<String> possibleReferencesForProperties,
                                final Set<String> definitionNamesToExtendBlackDuckView, final Set<String> definitionNamesToExtendBlackDuckResponse, final DefinitionLinks definitionLinks, final SwaggerEnumsParser swaggerEnumsParser) {
        swaggerDefinitions.each {
            try {
                File viewFile = baseDirectory
                final Set imports = new HashSet<>()

                FreemarkerComponentType freemarkerComponentType = FreemarkerComponentType.COMPONENT
                if (definitionNamesToExtendBlackDuckView.contains(it.definitionName)) {
                    viewFile = new File(viewFile, ModelCreator.VIEW_DIRECTORY);
                    freemarkerComponentType = FreemarkerComponentType.VIEW
                } else if (definitionNamesToExtendBlackDuckResponse.contains(it.definitionName)) {
                    viewFile = new File(viewFile, ModelCreator.RESPONSE_DIRECTORY);
                    freemarkerComponentType = FreemarkerComponentType.RESPONSE
                } else {
                    viewFile = new File(viewFile, ModelCreator.COMPONENT_DIRECTORY);
                }
                viewFile = new File(viewFile, it.definitionName + ".java")

                FreemarkerComponent freemarkerComponent = new FreemarkerComponent(imports, freemarkerComponentType)
                final Map<String, Object> model = new HashMap<>();
                model.put("className", it.definitionName);
                model.put("baseClass", freemarkerComponent.baseClass);
                model.put("viewPackage", freemarkerComponent.viewPackage)

                final Map<String, String> definitionLinksToConstants = definitionLinks.getLinksToJavaConstants(it.definitionName)
                if (definitionLinksToConstants != null && !definitionLinksToConstants.empty) {
                    model.put("hasLinks", true)
                    final List links = new ArrayList<>()
                    model.put("links", links)
                    definitionLinksToConstants.each { link, constant ->
                        Map<String, Object> linkModel = new HashMap<>();
                        linkModel.put("label", link);
                        linkModel.put("javaConstant", constant);

                        final String resultClass = definitionLinks.getResultClass(it.definitionName, link)
                        if (StringUtils.isNotBlank(resultClass)) {
                            model.put('hasLinksWithResults', true);
                            String importPackage = definitionLinks.getFullyQualifiedClassName(resultClass);
                            if (StringUtils.isNotBlank(importPackage)) {
                                imports.add(importPackage);
                            }
                            imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + "LinkResponse");
                            linkModel.put("resultClass", resultClass);
                            if ("String".equals(resultClass)) {
                                addImport(imports, "LinkStringResponse")
                                linkModel.put("linkType", "LinkStringResponse");
                            } else if (definitionLinks.canHaveManyResults(it.definitionName, link)) {
                                addImport(imports, "LinkMultipleResponses")
                                linkModel.put("hasMultipleResults", true);
                                linkModel.put("linkType", "LinkMultipleResponses<${resultClass}>");
                            } else {
                                addImport(imports, "LinkSingleResponse")
                                linkModel.put("linkType", "LinkSingleResponse<${resultClass}>");
                            }
                        }
                        links.add(linkModel)
                    }
                }

                List<Map<String, Object>> fields = new ArrayList<>();
                model.put("classFields", fields);

                it.definitionProperties.each { property ->
                    if (shouldProcessProperty(it.definitionName, property.name)) {
                        Map<String, Object> propertyModel = new HashMap<>();
                        propertyModel.put("name", property.name);
                        propertyModel.put("snakeCaseName", property.name.replaceAll(/[A-Z]/) { m -> "_${m}" }.toUpperCase());
                        FullyQualifiedClassName fullyQualifiedClassName = property.getFullyQualifiedClassName(imports, definitionLinks, swaggerEnumsParser, possibleReferencesForProperties);
                        String propertyType = fullyQualifiedClassName.fullyQualifiedClassName;
                        String importPackage = definitionLinks.getFullyQualifiedClassName(propertyType)
                        if (StringUtils.isNotBlank(importPackage)) {
                            imports.add(importPackage);
                        }
                        propertyModel.put("type", propertyType);
                        propertyModel.put("rawType", fullyQualifiedClassName.rawType);
                        propertyModel.put("isList", fullyQualifiedClassName.list);
                        fields.add(propertyModel)
                    }
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

    // for NotificationView and UserNotificationView we have to omit the
    // 'content' property because the swagger claims it is a String when in
    // fact it is an object and this discrepancy breaks gson parsing
    private boolean shouldProcessProperty(String definitionName, String propertyName) {
        if (('NotificationView' == definitionName || 'NotificationUserView' == definitionName) && ('content' == propertyName)) {
            return false;
        }
        return true;
    }

    private void addImport(Set imports, String apiClass) {
        imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + apiClass);
    }

}