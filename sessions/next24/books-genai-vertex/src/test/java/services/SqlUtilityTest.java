package services;

import org.junit.jupiter.api.Test;
import utility.SqlUtility;

import static org.junit.jupiter.api.Assertions.*;

public class SqlUtilityTest {

    @Test
    void testReplaceSpacesWithUnderscores() {
        String input = "John_Doe";
        String expected = "John Doe";
        String result = SqlUtility.replaceUnderscoresWithSpaces(input);
        assertEquals(expected, result);  
    }

    @Test
    void testEmptyString() {
        String input = "";
        String expected = "";
        String result = SqlUtility.replaceUnderscoresWithSpaces(input);
        assertEquals(expected, result);
    }

    @Test
    void testNullInput() {
        String input = null;
        assertEquals( SqlUtility.replaceUnderscoresWithSpaces(input), "" );
    }

    @Test
    void testMultipleSpaces() {
        String input = "This__has__multiple_spaces ";
        String expected = "This  has  multiple spaces";
        String result = SqlUtility.replaceUnderscoresWithSpaces(input);
        assertEquals(expected, result);
    }
    // More test cases below...
}