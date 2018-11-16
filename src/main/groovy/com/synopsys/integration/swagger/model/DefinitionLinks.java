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

    public DefinitionLinks(final List<DefinitionLinkEntry> linkEntries, final Set<String> allDefinitionNames, final Set<String> definitionNamesToExtendBlackDuckView, final Set<String> definitionNamesToExtendBlackDuckResponse,
            final Set<String> enumNames) {
        linkEntries.forEach(linkEntry -> {
            final String definitionName = linkEntry.getDefinitionName();

            if (!namesToEntries.containsKey(definitionName)) {
                namesToEntries.put(definitionName, new ArrayList<>());
            }
            namesToEntries.get(definitionName).add(linkEntry);

            if (!namesToLinksToJavaConstants.containsKey(definitionName)) {
                namesToLinksToJavaConstants.put(definitionName, new HashMap<>());
            }
            final String link = linkEntry.getLink();
            final String constant = convertLinkToJavaConstant(link);
            namesToLinksToJavaConstants.get(definitionName).put(link, constant);

            if (!namesToLinksToHasManyResults.containsKey(definitionName)) {
                namesToLinksToHasManyResults.put(definitionName, new HashMap<>());
            }
            namesToLinksToHasManyResults.get(definitionName).put(link, linkEntry.getCanHaveManyResults());

            if (!namesToLinksToResultClasses.containsKey(definitionName)) {
                namesToLinksToResultClasses.put(definitionName, new HashMap<>());
            }
            final String resultClass = linkEntry.getResultClass();
            namesToLinksToResultClasses.get(definitionName).put(link, resultClass);
        });

        allDefinitionNames.forEach(definitionName -> {
            if (definitionNamesToExtendBlackDuckView.contains(definitionName)) {
                namesToFullyQualifiedClassNames.put(definitionName, String.format("%s.%s", ModelCreator.VIEW_PACKAGE, definitionName));
            } else if (definitionNamesToExtendBlackDuckResponse.contains(definitionName)) {
                namesToFullyQualifiedClassNames.put(definitionName, String.format("%s.%s", ModelCreator.RESPONSE_PACKAGE, definitionName));
            } else {
                namesToFullyQualifiedClassNames.put(definitionName, String.format("%s.%s", ModelCreator.COMPONENT_PACKAGE, definitionName));
            }
        });
        enumNames.forEach(enumName -> {
            namesToFullyQualifiedClassNames.put(enumName, String.format("%s.%s", ModelCreator.ENUM_PACKAGE, enumName));
        });
    }

    public Map<String, String> getLinksToJavaConstants(final String definitionName) {
        return namesToLinksToJavaConstants.get(definitionName);
    }

    public boolean canHaveManyResults(final String definitionName, final String link) {
        return namesToLinksToHasManyResults.get(definitionName).get(link);
    }

    public String getResultClass(final String definitionName, final String link) {
        return namesToLinksToResultClasses.get(definitionName).get(link);
    }

    public String getFullyQualifiedClassName(final String name) {
        return namesToFullyQualifiedClassNames.get(name);
    }

    private String convertLinkToJavaConstant(final String link) {
        return link.replaceAll("[^A-Za-z0-9]", "_").toUpperCase() + "_LINK";
    }

}
