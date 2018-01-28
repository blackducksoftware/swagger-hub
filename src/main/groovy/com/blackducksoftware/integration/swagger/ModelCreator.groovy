package com.blackducksoftware.integration.swagger;

import java.nio.charset.StandardCharsets;

import com.blackducksoftware.integration.swagger.model.SwaggerDefinition
import com.blackducksoftware.integration.swagger.parser.SwaggerDefinitions
import com.blackducksoftware.integration.swagger.parser.SwaggerEnums
import com.blackducksoftware.integration.swagger.parser.SwaggerProperties
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

        final SwaggerEnums swaggerEnums = new SwaggerEnums()
        final SwaggerProperties swaggerProperties = new SwaggerProperties(swaggerEnums)
        final SwaggerDefinitions swaggerDefinitions = new SwaggerDefinitions(swaggerProperties);
        final Map<String, SwaggerDefinition> allObjectDefinitions = swaggerDefinitions.getDefinitionsFromJson(swaggerJson)

        logPossibleErrors(swaggerDefinitions, swaggerProperties, allObjectDefinitions);

        final Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setClassForTemplateLoading(ModelCreator.class, "/");
        configuration.setDefaultEncoding("UTF-8");
        Template enumTemplate = configuration.getTemplate("hubEnum.ftl");
        Template viewTemplate = configuration.getTemplate("hubView.ftl");
        JavaTypeMapper javaTypeMapper = new JavaTypeMapper();

        File enumBaseDirectory = new File(ModelCreator.BASE_DIRECTORY + ModelCreator.ENUM_DIRECTORY);
        File viewBaseDirectory = new File(ModelCreator.BASE_DIRECTORY + ModelCreator.VIEW_DIRECTORY);
        enumBaseDirectory.mkdirs();
        viewBaseDirectory.mkdirs();
        createEnumFiles(enumBaseDirectory, enumTemplate, swaggerEnums.enumNameToValues);
        createViewFiles(viewBaseDirectory, viewTemplate, new ArrayList<>(allObjectDefinitions.values()), javaTypeMapper);
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

    public static void createViewFiles(File baseDirectory, Template template, List<SwaggerDefinition> swaggerDefinitions, JavaTypeMapper javaTypeMapper) {
        swaggerDefinitions.each {
            File viewFile = new File(baseDirectory, it.definitionName + ".java");

            final Map<String, Object> model = new HashMap<>();
            model.put("viewPackage", ModelCreator.VIEW_PACKAGE)
            model.put("className", it.definitionName);
            List<Map<String, Object>> fields = new ArrayList<>();
            model.put("classFields", fields);

            it.definitionProperties.each { property ->
                Map<String, Object> propertyModel = new HashMap<>();
                propertyModel.put("name", property.name);
                propertyModel.put("type", javaTypeMapper.getFullyQualifiedClassName(it.definitionName, property));
                fields.add(propertyModel)
            }

            final FileWriter fileWriter = new FileWriter(viewFile);
            template.process(model, fileWriter);
        }
    }

    public static logPossibleErrors(SwaggerDefinitions swaggerDefinitions, SwaggerProperties swaggerProperties, Map<String, SwaggerDefinition> allObjectDefinitions) {
        println "Unknown definitions (possible problems) (${swaggerDefinitions.unknownDefinitionNames.size()}):"
        swaggerDefinitions.unknownDefinitionNames.toSorted { a, b ->
            a <=> b
        }.each { println it }
        println '---------------------------------------'

        println "Unknown property fields (possible problems) (${swaggerProperties.unknownPropertyFields.size()}):"
        swaggerProperties.unknownPropertyFields.toSorted { a, b ->
            a <=> b
        }.each { println it }
        println '---------------------------------------'

        println "Found definitions (${swaggerDefinitions.definitionNames.size()}):"
        swaggerDefinitions.definitionNames.toSorted { a, b ->
            a <=> b
        }.each { println it }
        println '---------------------------------------'

        println "Found types/formats:"
        swaggerProperties.propertyTypeToFormats.each { k, v ->
            println "${k}: formats(${v.join(', ')})"
        }
        println '---------------------------------------'

        allObjectDefinitions.each { k,v -> println v }
    }
}