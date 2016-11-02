/*
 * Copyright (C) 2016 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.swagger;

import java.util.Map;

import com.google.gson.JsonObject;

public class SwaggerDefinition {
    private String name;

    private Map<String, JsonObject> properties;

    public SwaggerDefinition(String name, Map<String, JsonObject> properties) {
        super();
        this.name = name;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public Map<String, JsonObject> getProperties() {
        return properties;
    }

}
