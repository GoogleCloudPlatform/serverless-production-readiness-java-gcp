/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package services.utility;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PromptUtility {
    public static String formatPromptBookKeywords(List<String> keywords) {
        // Check for an empty topics list
        List<String> params = keywords.stream()
                .filter(Objects::nonNull)  // Filters out null values
                .filter(p -> p instanceof String && !((String) p).isEmpty()) // Filters out empty strings
                .collect(Collectors.toList());

        if (params==null || params.isEmpty()) {
            return "Find the paragraphs mentioning any topic in the book."; // Or other default message
        }

        // Join the topics with commas
        String joinedTopics = String.join(", ", params);

        // Use String.format to insert the joined topics into the prompt
        return String.format("Find the paragraphs mentioning keywords in the following list: {%s} in the book.", joinedTopics);
    }

    public static String formatPromptBookAnalysis(List<Map<String, Object>> bookPages, List<String> keywords) {
        String promptBookAnalysis = "Provide an analysis of the %s by %s " +
                "with the skills of a literary critic." +
                "What factor do the following %s " +
                "play in the narrative of the book. " +
                "Please use these paragraphs delimited by triple backquotes from the book :\n" +
                "```%s```";

        // Check for an empty topics list
        List<String> params = keywords.stream()
                .filter(Objects::nonNull)  // Filters out null values
                .filter(k -> k instanceof String && !((String) k).isEmpty()) // Filters out empty strings
                .collect(Collectors.toList());

        if ( (params==null || params.isEmpty()) && bookPages.isEmpty()) {
            return ""; // Or other default message
        }

        System.out.println(params);

        String book = (String) bookPages.get(0).get("title");
        String author = (String) bookPages.get(0).get("author");
        String context = "";
        for(Map<String, Object> page: bookPages) {
            context += page.get("page")+" ";
        }

        return String.format(promptBookAnalysis, book, author, String.join(", ", params), context);
    }

}
