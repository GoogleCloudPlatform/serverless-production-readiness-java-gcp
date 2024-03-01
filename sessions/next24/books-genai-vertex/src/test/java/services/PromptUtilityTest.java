package services;

import org.junit.jupiter.api.Test;
import utility.FileUtility;
import utility.PromptUtility;

import java.util.List;

import static org.junit.Assert.assertEquals;

class PromptUtilityTest {

    @Test
    public void testFormatPrompt() {
        String nothing = null;
        Object[] args = {"adventure", nothing, "coming-of-age"};
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