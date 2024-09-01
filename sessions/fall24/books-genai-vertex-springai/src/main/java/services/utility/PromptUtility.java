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
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.Resource;
import services.web.data.BookRequest;


public class PromptUtility {

    private static final Logger logger = LoggerFactory.getLogger(PromptUtility.class);
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


    public static String formatPromptTF(List<Map<String, Object>> responseDoc, String promptTransformTF, String script) {
        // Check for an empty topics list
        Map<Integer, String> sortByPageNumber = getSortedPagesBasedonPageNumber(responseDoc);
        String context="";
        for(String page: sortByPageNumber.values()) {
            context += page + "\n";
        }
        return String.format(promptTransformTF, script, context);
    }


    public static Map<Integer, String> getSortedPagesBasedonPageNumber(List<Map<String, Object>> responseDoc) {
        // Check for an empty topics list
        Map<Integer, String> sortByPageNumber = new TreeMap<>(); // TreeMap to automatically sort by key
        int i =0;
        for(Map<String, Object> page: responseDoc) {
            sortByPageNumber.put(Integer.parseInt(page.get("page_number").toString()), (String) page.get("page"));
            i++;
            //Get top 5 results
            if(i==5) break;
        }

        String context = "";
        for(String page: sortByPageNumber.values()) {
            context += page + "\n";
        }
        return sortByPageNumber;
    }

    public static Message formatPromptBookAnalysis(Resource analysisUserMessage,
                                                   BookRequest bookRequest,
                                                   List<Map<String, Object>> bookPages,
                                                   List<String> keywords) {
        // Check for an empty topics list
        List<String> params = keywords.stream()
            .filter(Objects::nonNull)  // Filters out null values
            .filter(k -> k instanceof String && !((String) k).isEmpty()) // Filters out empty strings
            .collect(Collectors.toList());

        if ( (params==null || params.isEmpty()) && bookPages.isEmpty()) {
            return new UserMessage("Not information supplied to build a UserMessage. Missing data");
        }

        logger.info(params+"");

        String context = "";
        for(Map<String, Object> page: bookPages) {
            context += page.get("page")+" ";
        }

        PromptTemplate userPromptTemplate = new PromptTemplate(analysisUserMessage);
        return userPromptTemplate.createMessage(
                Map.of("title", bookRequest.book(),
                        "author", bookRequest.author(),
                        "keywords", String.join(", ", params),
                        "pages", context));
    }
}
