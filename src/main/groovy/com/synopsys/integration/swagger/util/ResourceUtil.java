package com.synopsys.integration.swagger.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class ResourceUtil {
    public static final Charset DEFAULT_RESOURCE_ENCODING = StandardCharsets.UTF_8;

    public List<String> getLinesFromResource(final String resourcePath) throws IOException {
        return getLinesFromResource(resourcePath, DEFAULT_RESOURCE_ENCODING);
    }

    public List<String> getLinesFromResource(final String resourcePath, final Charset encoding) throws IOException {
        final InputStream inputStream = getResourceAsStream(resourcePath);
        return IOUtils.readLines(inputStream, encoding);
    }

    public InputStream getResourceAsStream(final String resourcePath) {
        final String validatedResourcePath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        return getClass().getResourceAsStream(validatedResourcePath);
    }
}
