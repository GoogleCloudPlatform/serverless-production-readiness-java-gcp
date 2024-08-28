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

import services.utility.PromptUtility;

import java.util.List;

import static org.junit.Assert.assertEquals;

class PromptUtilityTest {

    @Test
    public void testFormatPrompt() {
        List<String> topics = List.of("adventure", "animals", "coming-of-age", "");
        String expectedPrompt = "Find the paragraphs mentioning keywords in the following list: {adventure, animals, coming-of-age} in the book.";
        String actualPrompt = PromptUtility.formatPromptBookKeywords(topics);
        assertEquals(expectedPrompt, actualPrompt);
    }

    @Test
    public void testFormatPromptEmptyList() {
        List<String> topics = List.of();
        String expectedPrompt = "Find the paragraphs mentioning any topic in the book.";
        String actualPrompt = PromptUtility.formatPromptBookKeywords(topics);
        assertEquals(expectedPrompt, actualPrompt);
    }

}