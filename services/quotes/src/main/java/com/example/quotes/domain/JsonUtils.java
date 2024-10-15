package com.example.quotes.domain;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.boot.json.GsonJsonParser;

public class JsonUtils {

    public List<Quote> parseResponseToQuotes(String response) {
        System.out.println(response);
        // Find JSON string enclosed by ```json ... ```
        Pattern pattern = Pattern.compile("```json\\s*(.*?)\\s*```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            String jsonStr = matcher.group(1);
            try {
                // Parse the extracted JSON string
                String cleanedJsonString = jsonStr.replaceAll("\\\\n*", "");
                System.out.println(cleanedJsonString);
                Gson gson = new Gson();
                Type quoteListType = new TypeToken<List<Quote>>() {}.getType();
                return gson.fromJson(cleanedJsonString, quoteListType);
            } catch (Exception e) {
                System.out.println("Error: Invalid JSON format in response.");
                return null;
            }
        } else {
            System.out.println("Error: No JSON string found in response.");
        }
        // If no JSON found or parsing failed, return null
        return null;
    }
}