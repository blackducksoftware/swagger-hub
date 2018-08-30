package com.synopsys.integration.swagger

import com.synopsys.integration.swagger.parser.SwaggerEnumsParser
import org.junit.Test

import static org.junit.Assert.assertEquals

public class SwaggerEnumParserTest {
    @Test
    public void testRemovingDuplicateWords() {
        final SwaggerEnumsParser swaggerEnums = new SwaggerEnumsParser();
        String s = swaggerEnums.removeDuplicateWords("ASimpleSimpleTest");
        assertEquals("ASimpleTest", s);

        s = swaggerEnums.removeDuplicateWords("ASimpleSimpleSimpleTest");
        assertEquals("ASimpleTest", s);

        s = swaggerEnums.removeDuplicateWords("ASimpleSimpleSimpleTestTest");
        assertEquals("ASimpleTest", s);

        s = swaggerEnums.removeDuplicateWords("TrickyTrickyTestTest");
        assertEquals("TrickyTest", s);
    }

}
