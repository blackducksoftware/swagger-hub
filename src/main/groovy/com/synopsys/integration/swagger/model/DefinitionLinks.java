/**
 * swagger-hub
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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
package com.synopsys.integration.swagger.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.synopsys.integration.swagger.ModelCreator;

public class DefinitionLinks {
    private final Map<String, List<DefinitionLinkEntry>> namesToEntries = new HashMap<>();
    private final Map<String, Map<String, String>> namesToLinksToJavaConstants = new HashMap<>();
    private final Map<String, Map<String, Boolean>> namesToLinksToHasManyResults = new HashMap<>();
    private final Map<String, Map<String, String>> namesToLinksToResultClasses = new HashMap<>();
    private final Map<String, String> namesToFullyQualifiedClassNames = new HashMap<>();

    public DefinitionLinks(List<DefinitionLinkEntry> linkEntries, Set<String> allDefinitionNames, Set<String> definitionNamesToExtendBlackDuckView, Set<String> definitionNamesToExtendBlackDuckResponse,
            Set<String> enumNames, Set<String> definitionNamesMaintainedManually) {
        linkEntries.forEach(linkEntry -> {
            String definitionName = linkEntry.getDefinitionName();

            if (!namesToEntries.containsKey(definitionName)) {
                namesToEntries.put(definitionName, new ArrayList<>());
            }
            namesToEntries.get(definitionName).add(linkEntry);

            if (!namesToLinksToJavaConstants.containsKey(definitionName)) {
                namesToLinksToJavaConstants.put(definitionName, new HashMap<>());
            }
            String link = linkEntry.getLink();
            String constant = convertLinkToJavaConstant(link);
            namesToLinksToJavaConstants.get(definitionName).put(link, constant);

            if (!namesToLinksToHasManyResults.containsKey(definitionName)) {
                namesToLinksToHasManyResults.put(definitionName, new HashMap<>());
            }
            namesToLinksToHasManyResults.get(definitionName).put(link, linkEntry.getCanHaveManyResults());

            if (!namesToLinksToResultClasses.containsKey(definitionName)) {
                namesToLinksToResultClasses.put(definitionName, new HashMap<>());
            }
            String resultClass = linkEntry.getResultClass();
            namesToLinksToResultClasses.get(definitionName).put(link, resultClass);
        });

        allDefinitionNames.forEach(definitionName -> {
            String packagePrefix = null;
            if (definitionNamesMaintainedManually.contains(definitionName)) {
                if (definitionNamesToExtendBlackDuckView.contains(definitionName)) {
                    packagePrefix = ModelCreator.MANUAL_VIEW_PACKAGE;
                } else if (definitionNamesToExtendBlackDuckResponse.contains(definitionName)) {
                    packagePrefix = ModelCreator.MANUAL_RESPONSE_PACKAGE;
                } else {
                    packagePrefix = ModelCreator.MANUAL_COMPONENT_PACKAGE;
                }
            } else {
                if (definitionNamesToExtendBlackDuckView.contains(definitionName)) {
                    packagePrefix = ModelCreator.GENERATED_VIEW_PACKAGE;
                } else if (definitionNamesToExtendBlackDuckResponse.contains(definitionName)) {
                    packagePrefix = ModelCreator.GENERATED_RESPONSE_PACKAGE;
                } else {
                    packagePrefix = ModelCreator.GENERATED_COMPONENT_PACKAGE;
                }
            }

            namesToFullyQualifiedClassNames.put(definitionName, String.format("%s.%s", packagePrefix, definitionName));
        });
        enumNames.forEach(enumName -> {
            namesToFullyQualifiedClassNames.put(enumName, String.format("%s.%s", ModelCreator.ENUM_PACKAGE, enumName));
        });
    }

    public Map<String, String> getLinksToJavaConstants(String definitionName) {
        return namesToLinksToJavaConstants.get(definitionName);
    }

    public boolean canHaveManyResults(String definitionName, String link) {
        return namesToLinksToHasManyResults.get(definitionName).get(link);
    }

    public String getResultClass(String definitionName, String link) {
        return namesToLinksToResultClasses.get(definitionName).get(link);
    }

    public String getFullyQualifiedClassName(String name) {
        return namesToFullyQualifiedClassNames.get(name);
    }

    private String convertLinkToJavaConstant(String link) {
        return link.replaceAll("[^A-Za-z0-9]", "_").toUpperCase() + "_LINK";
    }

}
