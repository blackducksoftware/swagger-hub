package com.blackducksoftware.integration.swagger.creator;

import org.apache.commons.lang3.StringUtils

import com.blackducksoftware.integration.swagger.ModelCreator;
import com.blackducksoftware.integration.swagger.model.DefinitionLinks;
import com.blackducksoftware.integration.swagger.model.SwaggerDefinition;
import com.blackducksoftware.integration.swagger.parser.SwaggerEnumsParser;

import freemarker.template.Template;

public class ComponentCreator {

    public void createViewFiles(final File baseDirectory, final Template template, final List<SwaggerDefinition> swaggerDefinitions, final Set<String> possibleReferencesForProperties, final Set<String> definitionNamesToExtendHubView, final Set<String> definitionNamesToExtendHubResponse, final DefinitionLinks definitionLinks, final SwaggerEnumsParser swaggerEnumsParser) {
        swaggerDefinitions.each {
            try {
                File viewFile = baseDirectory
                final Set imports = new HashSet<>()

                FreemarkerComponentType freemarkerComponentType = FreemarkerComponentType.COMPONENT
                if (definitionNamesToExtendHubView.contains(it.definitionName)) {
                    viewFile = new File(viewFile, ModelCreator.VIEW_DIRECTORY);
                    freemarkerComponentType = FreemarkerComponentType.VIEW
                } else if (definitionNamesToExtendHubResponse.contains(it.definitionName)) {
                    viewFile = new File(viewFile, ModelCreator.RESPONSE_DIRECTORY);
                    freemarkerComponentType = FreemarkerComponentType.RESPONSE
                } else {
                    viewFile = new File(viewFile, ModelCreator.MODEL_DIRECTORY);
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
}
