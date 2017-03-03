package com.blackducksoftware.integration.swagger;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SwaggerEnumParserTest {
    @Test
    public void testRemovingDuplicateWords() {
        final SwaggerEnumParser swaggerEnumParser = new SwaggerEnumParser();
        String s = swaggerEnumParser.removeDuplicateWords("aSimpleSimpleTest");
        assertEquals("aSimpleTest", s);

        s = swaggerEnumParser.removeDuplicateWords("aSimpleSimpleSimpleTest");
        assertEquals("aSimpleTest", s);

        s = swaggerEnumParser.removeDuplicateWords("aSimpleSimpleSimpleTestTest");
        assertEquals("aSimpleTest", s);

        s = swaggerEnumParser.removeDuplicateWords("trickyTrickyTestTest");
        assertEquals("trickyTest", s);
    }

}
