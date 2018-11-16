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

import com.synopsys.integration.util.Stringable;

public class ApiPath extends Stringable {
    public final String path;
    public final boolean hasManyResults;
    public final String resultClass;

    public ApiPath(final String path, final boolean hasManyResults, final String resultClass) {
        this.path = path;
        this.hasManyResults = hasManyResults;
        this.resultClass = resultClass;
    }

    public String getJavaConstant() {
        return path.replace("/api/", "").replaceAll("[^A-Za-z0-9]", "_").toUpperCase() + "_LINK";
    }

}
