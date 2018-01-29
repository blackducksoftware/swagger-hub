package com.blackducksoftware.integration.swagger.model;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.swagger.ModelCreator;
import com.blackducksoftware.integration.swagger.parser.SwaggerDefinitionsParser;
import com.blackducksoftware.integration.util.Stringable;
import com.google.gson.JsonObject;

/*
 * There are 3 types of properties:
 * 1) type and format - this is for booleans, strings, dates
 * 2) ref - this is when another model is referenced
 * 3) enumType - this is when the property represents an enum value
 */
public class SwaggerDefinitionProperty extends Stringable {
    private static final String OPTIONAL_START = "Optional" + SwaggerDefinitionsParser.CONTAINER_START_MARKER;
    private static final String OPTIONAL_END = SwaggerDefinitionsParser.CONTAINER_END_MARKER;
    private static final String COLLECTION_START = "Collection" + SwaggerDefinitionsParser.CONTAINER_START_MARKER;
    private static final String COLLECTION_END = SwaggerDefinitionsParser.CONTAINER_END_MARKER;
    private static final String LIST_START = "List" + SwaggerDefinitionsParser.CONTAINER_START_MARKER;
    private static final String LIST_END = SwaggerDefinitionsParser.CONTAINER_END_MARKER;

    public String name;
    public String description;
    public boolean readOnly;

    public String propertyType;
    public String format;

    public String ref;

    public String enumType;
    public JsonObject propertyJsonObject;

    public String getFullyQualifiedClassName(final Set<String> possibleReferencesForProperties) throws Exception {
        if (StringUtils.isNotBlank(enumType)) {
            return ModelCreator.ENUM_PACKAGE + "." + enumType;
        } else if (StringUtils.isNotBlank(ref)) {
            String reference = ref.replace("#/definitions/", "");
            if (reference.startsWith(OPTIONAL_START)) {
                final int start = reference.indexOf(OPTIONAL_START) + OPTIONAL_START.length();
                final int end = reference.lastIndexOf(OPTIONAL_END);
                reference = reference.substring(start, end);
            }
            boolean isList = false;
            if (reference.startsWith(COLLECTION_START)) {
                final int start = reference.indexOf(COLLECTION_START) + COLLECTION_START.length();
                final int end = reference.lastIndexOf(COLLECTION_END);
                reference = reference.substring(start, end);
                isList = true;
            } else if (reference.startsWith(LIST_START)) {
                final int start = reference.indexOf(LIST_START) + LIST_START.length();
                final int end = reference.lastIndexOf(LIST_END);
                reference = reference.substring(start, end);
                isList = true;
            }
            final String converted = convertSwaggerPrimitiveToJava(reference);
            if (converted != null) {
                return isList ? String.format("java.util.List<%s>", converted) : converted;
            }
            if (!possibleReferencesForProperties.contains(reference)) {
                throw new Exception("Not a known java type: " + reference + " in " + toString());
            }
            return isList ? String.format("java.util.List<%s>", reference) : reference;
        } else if ("array".equals(propertyType) && propertyJsonObject.has("items") && propertyJsonObject.getAsJsonObject("items").has("$ref")) {
            final String javaType = propertyJsonObject.getAsJsonObject("items").get("$ref").getAsString().replace("#/definitions/", "");
            if (!possibleReferencesForProperties.contains(javaType)) {
                throw new Exception("Not a known java type: " + javaType + " in " + toString());
            }
            return String.format("java.util.List<%s>", javaType);
        } else if ("array".equals(propertyType) && propertyJsonObject.has("items") && propertyJsonObject.getAsJsonObject("items").has("type")) {
            final String javaType = propertyJsonObject.getAsJsonObject("items").get("type").getAsString();
            if ("string".equals(javaType)) {
                return "java.util.List<String>";
            }
        } else {
            final String converted = convertSwaggerPrimitiveToJava(propertyType);
            if (converted != null) {
                return converted;
            }
        }

        throw new Exception("Couldn't determine the type:" + toString());
    }

    private String convertSwaggerPrimitiveToJava(final String swaggerPrimitive) throws Exception {
        if ("object".equals(swaggerPrimitive) && format == null) {
            return "String";
        }
        if ("number".equals(swaggerPrimitive) && "double".equals(format)) {
            return "java.math.BigDecimal";
        } else if ("integer".equals(swaggerPrimitive) && "int32".equals(format)) {
            return "Integer";
        } else if ("integer".equals(swaggerPrimitive) && "int64".equals(format)) {
            return "Long";
        } else if ("boolean".equals(swaggerPrimitive)) {
            return "Boolean";
        } else if ("DateTime".equals(swaggerPrimitive)) {
            return "java.util.Date";
        } else if ("string".equals(swaggerPrimitive)) {
            if ("date-time".equals(format)) {
                return "java.util.Date";
            } else {
                return "String";
            }
        }

        return null;
    }
}