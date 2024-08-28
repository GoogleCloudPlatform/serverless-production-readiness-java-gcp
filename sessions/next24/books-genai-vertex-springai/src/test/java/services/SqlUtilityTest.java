/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package services;

import org.junit.jupiter.api.Test;

import services.utility.SqlUtility;

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