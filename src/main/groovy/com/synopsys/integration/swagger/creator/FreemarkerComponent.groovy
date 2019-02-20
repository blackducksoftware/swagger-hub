/*
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
package com.synopsys.integration.swagger.creator

import com.synopsys.integration.swagger.ModelCreator

class FreemarkerComponent {
    private String baseClass
    private String viewPackage
    private Set<String> imports

    public FreemarkerComponent(Set<String> imports, FreemarkerComponentType freemarkerComponentType) {
        this.imports = imports
        setTypeAndPopulateImports(freemarkerComponentType)
    }

    public void setTypeAndPopulateImports(FreemarkerComponentType freemarkerComponentType) {
        if (FreemarkerComponentType.COMPONENT == freemarkerComponentType) {
            populateComponent(ModelCreator.COMPONENT_PACKAGE_SUFFIX)
        } else if (FreemarkerComponentType.RESPONSE == freemarkerComponentType) {
            populateComponent(ModelCreator.RESPONSE_PACKAGE_SUFFIX)
        } else if (FreemarkerComponentType.VIEW == freemarkerComponentType) {
            populateComponent(ModelCreator.VIEW_PACKAGE_SUFFIX)
        } else {
            throw new Exception("Unexpected freemarker type: ${freemarkerComponentType}");
        }
    }

    private void populateComponent(String packageSuffix) {
        viewPackage = ModelCreator.GENERATED_PACKAGE_PREFIX + packageSuffix
        baseClass = 'BlackDuck' + packageSuffix[1..-1].capitalize()
        imports.add(ModelCreator.API_CORE_PACKAGE_PREFIX + "." + baseClass);
    }
}
