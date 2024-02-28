package utility;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PromptUtility {
    public static String formatPrompt(List<String> topics) {
        // Check for an empty topics list
        List<String> params = topics.stream()
                .filter(Objects::nonNull)  // Filters out null values
                .filter(p -> p instanceof String && !((String) p).isEmpty()) // Filters out empty strings
                .collect(Collectors.toList());

        if (params==null || params.isEmpty()) {
            return "Find the paragraphs mentioning any topic in the book"; // Or other default message
        }

        // Join the topics with commas
        String joinedTopics = String.join(", ", params);

        // Use String.format to insert the joined topics into the prompt
        return String.format("Find the paragraphs mentioning %s in the book", joinedTopics);
    }

}
