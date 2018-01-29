package com.blackducksoftware.integration.swagger;

import java.nio.charset.StandardCharsets;

import com.blackducksoftware.integration.swagger.model.SwaggerDefinition
import com.blackducksoftware.integration.swagger.parser.SwaggerDefinitionsParser
import com.blackducksoftware.integration.swagger.parser.SwaggerEnumsParser
import com.blackducksoftware.integration.swagger.parser.SwaggerPropertiesParser
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import freemarker.template.Configuration
import freemarker.template.Template

public class ModelCreator {
    public static final String BASE_DIRECTORY = "/Users/ekerwin/Documents/generated";
    public static final String ENUM_DIRECTORY = "/com/blackducksoftware/integration/hub/model/enumeration";
    public static final String VIEW_DIRECTORY = "/com/blackducksoftware/integration/hub/model/view";
    public static final String ENUM_PACKAGE = "com.blackducksoftware.integration.hub.model.enumeration";
    public static final String VIEW_PACKAGE = "com.blackducksoftware.integration.hub.model.view";

    public static void main(final String[] args) throws Exception {
        final File jsonFile = new File(ModelCreator.class.getClassLoader().getResource("api-docs_4.4.0.json").toURI());
        final FileInputStream jsonFileInputStream = new FileInputStream(jsonFile);
        final InputStreamReader jsonInputStreamReader = new InputStreamReader(jsonFileInputStream, StandardCharsets.UTF_8);

        final JsonParser jsonParser = new JsonParser();
        final JsonObject swaggerJson = jsonParser.parse(jsonInputStreamReader).getAsJsonObject();

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
        createViewFiles(viewBaseDirectory, viewTemplate, new ArrayList<>(allObjectDefinitions.values()), possibleReferencesForProperties);
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

    public static void createViewFiles(File baseDirectory, Template template, List<SwaggerDefinition> swaggerDefinitions, Set<String> possibleReferencesForProperties) {
        swaggerDefinitions.each {
            try {
                File viewFile = new File(baseDirectory, it.definitionName + ".java");

                final Map<String, Object> model = new HashMap<>();
                model.put("viewPackage", ModelCreator.VIEW_PACKAGE)
                model.put("className", it.definitionName);
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