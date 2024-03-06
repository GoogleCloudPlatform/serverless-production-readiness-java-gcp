package services.utility;

import org.springframework.boot.json.GsonJsonParser;

import java.util.Map;

public class JsonUtility {
    public static Map<String, Object> parseJsonToMap(String mixedString) {
        // Find the start and end of the JSON string
        int startIndex = mixedString.indexOf("{");
        int endIndex = mixedString.lastIndexOf("}") + 1;
        String jsonString = mixedString.substring(startIndex, endIndex);

        // Parse the JSON string
        GsonJsonParser parser = new GsonJsonParser();
        return parser.parseMap(jsonString);
    }
}
