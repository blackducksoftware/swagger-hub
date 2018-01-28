package com.blackducksoftware.integration.swagger.parser

import com.blackducksoftware.integration.swagger.model.SwaggerDefinitionProperty
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class SwaggerProperties {
    private static final List<String> knownPropertyFields = [
        'format',
        'type',
        'description',
        'readOnly',
        'enum',
        'items',
        '$ref'
    ]

    private final SwaggerEnums swaggerEnums

    def propertyTypeToFormats = new HashMap<String, Set<String>>()
    def unknownPropertyFields = new HashSet<String>()

    public SwaggerProperties(SwaggerEnums swaggerEnums) {
        this.swaggerEnums = swaggerEnums
    }

    public List<SwaggerDefinitionProperty> getPropertiesFromJson(String definitionName, final JsonObject definitionJsonObject) {
        List<SwaggerDefinitionProperty> swaggerDefinitionProperties = []
        if (definitionJsonObject.has("properties")) {
            JsonObject propertiesJsonObject = definitionJsonObject.get("properties").getAsJsonObject()
            for (final Map.Entry<String, JsonElement> property : propertiesJsonObject.entrySet()) {
                String propertyName = property.getKey();
                if (!property.getValue().isJsonObject()) {
                    throw new Exception("each property should be an object but ${propertyName} was not")
                }
                JsonObject propertyJsonObject = property.getValue().getAsJsonObject()
                SwaggerDefinitionProperty swaggerDefinitionProperty = new SwaggerDefinitionProperty()
                swaggerDefinitionProperty.name = propertyName
                populatePropertyFields(swaggerDefinitionProperty, propertyJsonObject)
                swaggerEnums.populateEnumField(swaggerDefinitionProperty, definitionName, propertyName, propertyJsonObject)
                if (!propertyTypeToFormats.containsKey(swaggerDefinitionProperty.propertyType)) {
                    propertyTypeToFormats.put(swaggerDefinitionProperty.propertyType, new HashSet<>())
                }
                propertyTypeToFormats.get(swaggerDefinitionProperty.propertyType).add(swaggerDefinitionProperty.format)
                swaggerDefinitionProperties.add(swaggerDefinitionProperty)
            }
        }
        return swaggerDefinitionProperties
    }

    private void populatePropertyFields(SwaggerDefinitionProperty property, JsonObject propertyJsonObject) {
        for (final Map.Entry<String, JsonElement> field : propertyJsonObject.entrySet()) {
            if (!knownPropertyFields.contains(field.key)) {
                unknownPropertyFields.add(field.key)
            } else if ('format' == field.key) {
                property.format = field.value.asString
            } else if ('type' == field.key) {
                property.propertyType = field.value.asString
            } else if ('description' == field.key) {
                property.description = field.value.asString
            } else if ('readOnly' == field.key) {
                property.readOnly = field.value.asString
            } else if ('$ref' == field.key) {
                property.ref = field.value.asString
            }
        }
        property.propertyJsonObject = propertyJsonObject
    }
}