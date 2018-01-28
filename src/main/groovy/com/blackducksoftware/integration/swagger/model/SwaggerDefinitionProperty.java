package com.blackducksoftware.integration.swagger.model;

import com.blackducksoftware.integration.util.Stringable;
import com.google.gson.JsonObject;

/*
 * There are 3 types of properties:
 * 1) type and format - this is for booleans, strings, dates
 * 2) ref - this is when another model is referenced
 * 3) enumType - this is when the property represents an enum value
 */
public class SwaggerDefinitionProperty extends Stringable {
    public String name;
    public String description;
    public boolean readOnly;

    public String propertyType;
    public String format;

    public String ref;

    public String enumType;
    public JsonObject propertyJsonObject;
}
