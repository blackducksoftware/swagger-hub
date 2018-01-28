package com.blackducksoftware.integration.swagger

import com.blackducksoftware.integration.swagger.model.SwaggerDefinitionProperty

/*
 string: formats(null, date-time, uuid)
 */
class JavaTypeMapper {
    public String getFullyQualifiedClassName(String definitionName, SwaggerDefinitionProperty swaggerDefinitionProperty) {
        if (swaggerDefinitionProperty.enumType) {
            return ModelCreator.ENUM_PACKAGE + "." + swaggerDefinitionProperty.enumType;
        } else if (swaggerDefinitionProperty.ref) {
            return swaggerDefinitionProperty.ref.replace('#/definitions/', '')
        } else if (swaggerDefinitionProperty.propertyType.equals("array") && swaggerDefinitionProperty.propertyJsonObject.has('items') && swaggerDefinitionProperty.propertyJsonObject.getAsJsonObject('items').has('$ref')) {
            String javaType = swaggerDefinitionProperty.propertyJsonObject.getAsJsonObject('items').get('$ref').getAsString().replace('#/definitions/', '')
            return "java.util.List<${javaType}>"
        } else if (swaggerDefinitionProperty.propertyType.equals("array") && swaggerDefinitionProperty.propertyJsonObject.has('items') && swaggerDefinitionProperty.propertyJsonObject.getAsJsonObject('items').has('type')) {
            String javaType = swaggerDefinitionProperty.propertyJsonObject.getAsJsonObject('items').get('type').getAsString()
            if ('string'.equals(javaType)) {
                return "java.util.List<String>"
            }
        } else if (swaggerDefinitionProperty.propertyType.equals("number") && swaggerDefinitionProperty.format.equals("double")) {
            return "java.math.BigDecimal";
        } else if (swaggerDefinitionProperty.propertyType.equals("integer")) {
            return "Long";
        } else if (swaggerDefinitionProperty.propertyType.equals("boolean")) {
            return "Boolean";
        } else if (swaggerDefinitionProperty.propertyType.equals("string")) {
            if (swaggerDefinitionProperty.format.equals("date-time")) {
                return "java.util.Date";
            } else {
                return "String";
            }
        }

        throw new Exception("Couldn't determine the type for ${definitionName}:" + swaggerDefinitionProperty.toString());
    }
}
