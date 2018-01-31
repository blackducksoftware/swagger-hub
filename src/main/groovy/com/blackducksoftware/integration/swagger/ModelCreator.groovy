package com.blackducksoftware.integration.swagger;

import java.nio.charset.StandardCharsets;

import com.blackducksoftware.integration.swagger.model.DefinitionLinkEntry
import com.blackducksoftware.integration.swagger.model.DefinitionLinks
import com.blackducksoftware.integration.swagger.model.SwaggerDefinition
import com.blackducksoftware.integration.swagger.parser.SwaggerDefinitionsParser
import com.blackducksoftware.integration.swagger.parser.SwaggerEnumsParser
import com.blackducksoftware.integration.swagger.parser.SwaggerPropertiesParser
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import freemarker.template.Configuration
import freemarker.template.Template

public class ModelCreator {
    public static final String BASE_DIRECTORY = "/Users/ekerwin/Documents/source/integration/hub-common-model/src/main/java";
    public static final String ENUM_DIRECTORY = "/com/blackducksoftware/integration/hub/model/generated/enumeration";
    public static final String VIEW_DIRECTORY = "/com/blackducksoftware/integration/hub/model/generated/view";
    public static final String ENUM_PACKAGE = "com.blackducksoftware.integration.hub.model.generated.enumeration";
    public static final String VIEW_PACKAGE = "com.blackducksoftware.integration.hub.model.generated.view";

    public static void main(final String[] args) throws Exception {
        final File jsonFile = new File(ModelCreator.class.getClassLoader().getResource("api-docs_4.4.0.json").toURI());
        final FileInputStream jsonFileInputStream = new FileInputStream(jsonFile);
        final InputStreamReader jsonInputStreamReader = new InputStreamReader(jsonFileInputStream, StandardCharsets.UTF_8);

        final JsonParser jsonParser = new JsonParser();
        final JsonObject swaggerJson = jsonParser.parse(jsonInputStreamReader).getAsJsonObject();

        final File definitionsWithMetaFile = new File(ModelCreator.class.getClassLoader().getResource("definitions_with_meta_in_response.txt").toURI());
        List<String> definitionLines = definitionsWithMetaFile.readLines()
        Set<String> definitionNamesToExtendHubView = new HashSet<>(definitionLines);

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

        final DefinitionLinks definitionLinks = new DefinitionLinks(linkEntries)
        final SwaggerEnumsParser swaggerEnumsParser = new SwaggerEnumsParser()
        final SwaggerPropertiesParser swaggerPropertiesParser = new SwaggerPropertiesParser(swaggerEnumsParser)
        final SwaggerDefinitionsParser swaggerDefinitionsParser = new SwaggerDefinitionsParser(swaggerPropertiesParser);
        final Map<String, SwaggerDefinition> allObjectDefinitions = swaggerDefinitionsParser.getDefinitionsFromJson(swaggerJson)

        logPossibleErrors(swaggerDefinitionsParser, swaggerPropertiesParser, allObjectDefinitions);
        Set<String> possibleReferencesForProperties = new HashSet<>();
        possibleReferencesForProperties.addAll(allObjectDefinitions.keySet());
        possibleReferencesForProperties.addAll(SwaggerDefinitionsParser.DEFINITIONS_TO_IGNORE_AND_CREATE_MANUALLY);

        final Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setClassForTemplateLoading(ModelCreator.class, "/");
        configuration.setDefaultEncoding("UTF-8");
        Template enumTemplate = configuration.getTemplate("hubEnum.ftl");
        Template viewTemplate = configuration.getTemplate("hubView.ftl");

        File enumBaseDirectory = new File(ModelCreator.BASE_DIRECTORY + ModelCreator.ENUM_DIRECTORY);
        File viewBaseDirectory = new File(ModelCreator.BASE_DIRECTORY + ModelCreator.VIEW_DIRECTORY);
        enumBaseDirectory.mkdirs();
        viewBaseDirectory.mkdirs();
        createEnumFiles(enumBaseDirectory, enumTemplate, swaggerEnumsParser.enumNameToValues);
        createViewFiles(viewBaseDirectory, viewTemplate, new ArrayList<>(allObjectDefinitions.values()), possibleReferencesForProperties, definitionNamesToExtendHubView, definitionLinks);
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

    public static void createViewFiles(File baseDirectory, Template template, List<SwaggerDefinition> swaggerDefinitions, Set<String> possibleReferencesForProperties, Set<String> definitionNamesToExtendHubView, DefinitionLinks definitionLinks) {
        swaggerDefinitions.each {
            try {
                File viewFile = new File(baseDirectory, it.definitionName + ".java");

                final Map<String, Object> model = new HashMap<>();
                model.put("viewPackage", ModelCreator.VIEW_PACKAGE)
                model.put("className", it.definitionName);
                if (definitionNamesToExtendHubView.contains(it.definitionName)) {
                    model.put("baseClass", "HubView");
                } else {
                    model.put("baseClass", "HubModel");
                }

                Map<String, String> definitionLinksToConstants = definitionLinks.getLinksToJavaConstants(it.definitionName)
                if (definitionLinksToConstants != null && !definitionLinksToConstants.empty) {
                    model.put("hasLinks", true)
                    List links = new ArrayList<>()
                    model.put("links", links)
                    definitionLinksToConstants.each { link, constant ->
                        Map<String, Object> linkModel = new HashMap<>();
                        linkModel.put("label", link);
                        linkModel.put("javaConstant", constant);
                        linkModel.put("resultClass", definitionLinks.getResultClass(it.definitionName, link));
                        if (definitionLinks.canHaveManyResults(it.definitionName, link)) {
                            linkModel.put("hasMultipleResults", true);
                            model.put('hasMultipleResultsLink', true);
                        }
                        links.add(linkModel)
                    }
                }

                List<Map<String, Object>> fields = new ArrayList<>();
                model.put("classFields", fields);

                it.definitionProperties.each { property ->
                    Map<String, Object> propertyModel = new HashMap<>();
                    propertyModel.put("name", property.name);
                    propertyModel.put("type", property.getFullyQualifiedClassName(possibleReferencesForProperties));
                    fields.add(propertyModel)
                }
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