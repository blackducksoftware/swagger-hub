package com.blackducksoftware.integration.swagger.model;

import com.blackducksoftware.integration.util.Stringable;

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
