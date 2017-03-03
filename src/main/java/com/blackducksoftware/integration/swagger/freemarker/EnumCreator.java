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
    private final String baseFilePath;

    private final Template template;

    public static void main(final String[] args)
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, URISyntaxException {
        final Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setClassForTemplateLoading(EnumCreator.class, "/");
        configuration.setDefaultEncoding("UTF-8");

        final File apiJsonFile = new File(EnumCreator.class.getClassLoader().getResource("api-docs_3.5.0.json").toURI());
        final EnumCreator enumCreator = new EnumCreator(
                "/Users/ekerwin/Documents/source/integration/hub-common-response/src/main/java/com/blackducksoftware/integration/hub/model/type",
                configuration);

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
