/**
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
package com.blackducksoftware.integration.swagger.freemarker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.blackducksoftware.integration.swagger.SwaggerDefinition;
import com.blackducksoftware.integration.swagger.SwaggerDefinitionsParser;
import com.blackducksoftware.integration.swagger.SwaggerEnumParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

public class EnumCreator {
    private static final String HUB_COMMON_RESPONSE_PATH = "/Users/ekerwin/Documents/source/integration/hub-common-response";

    private static final String PATH_TO_PLACE_ENUMS = HUB_COMMON_RESPONSE_PATH + "/src/main/java/com/blackducksoftware/integration/hub/model/enumeration";

    private final String baseFilePath;

    private final Template template;

    public static void main(final String[] args)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, URISyntaxException {
        final Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setClassForTemplateLoading(EnumCreator.class, "/");
        configuration.setDefaultEncoding("UTF-8");

        final File apiJsonFile = new File(EnumCreator.class.getClassLoader().getResource("api-docs_3.5.0.json").toURI());
        final EnumCreator enumCreator = new EnumCreator(PATH_TO_PLACE_ENUMS, configuration);

        final FileInputStream jsonFile = new FileInputStream(apiJsonFile);
        final String json = IOUtils.toString(jsonFile, "UTF-8");

        final JsonParser jsonParser = new JsonParser();
        final JsonObject swaggerJson = jsonParser.parse(json).getAsJsonObject();

        final SwaggerDefinitionsParser swaggerDefinitionsParser = new SwaggerDefinitionsParser();
        final List<SwaggerDefinition> allObjectDefinitions = swaggerDefinitionsParser.getAllObjectDefinitions(swaggerJson);

        final SwaggerEnumParser swaggerEnumParser = new SwaggerEnumParser();
        final Map<String, List<String>> enums = swaggerEnumParser.getEnumNameToValues(allObjectDefinitions);
        for (final Map.Entry<String, List<String>> entry : enums.entrySet()) {
            enumCreator.createEnumFile(entry.getKey(), entry.getValue());
        }
    }

    public EnumCreator(final String baseFilePath, final Configuration configuration)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        this.baseFilePath = baseFilePath;
        template = configuration.getTemplate("hubEnum.ftl");

        // in case the directory doesn't exist...
        new File(baseFilePath).mkdirs();
    }

    public void createEnumFile(final String enumClassName, final List<String> enumValues) throws TemplateException, IOException {
        final String filename = enumClassName + ".java";
        File enumFile = new File(baseFilePath);
        enumFile = new File(enumFile, filename);

        final Map<String, Object> model = new HashMap<>();
        model.put("enumClassName", enumClassName);
        model.put("enumValues", enumValues);

        final FileWriter fileWriter = new FileWriter(enumFile);
        template.process(model, fileWriter);
    }

}
