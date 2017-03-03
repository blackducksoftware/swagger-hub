package com.blackducksoftware.integration.swagger;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SwaggerEnumParserTest {
    @Test
    public void testRemovingDuplicateWords() {
        final SwaggerEnumParser swaggerEnumParser = new SwaggerEnumParser();
        String s = swaggerEnumParser.removeDuplicateWords("ASimpleSimpleTest");
        assertEquals("ASimpleTest", s);

        s = swaggerEnumParser.removeDuplicateWords("ASimpleSimpleSimpleTest");
        assertEquals("ASimpleTest", s);

        s = swaggerEnumParser.removeDuplicateWords("ASimpleSimpleSimpleTestTest");
        assertEquals("ASimpleTest", s);

        s = swaggerEnumParser.removeDuplicateWords("TrickyTrickyTestTest");
        assertEquals("TrickyTest", s);
    }

}
