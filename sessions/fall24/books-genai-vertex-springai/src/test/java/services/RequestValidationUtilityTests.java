package services;
import org.junit.jupiter.api.Test;
import services.utility.PromptUtility;
import services.utility.RequestValidationUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RequestValidationUtilityTests {

    @Test
    public void testValidateRequestSuccess() {
        Map<String, String> headers = new HashMap<>();

        headers.put("ce-id", "value1");
        headers.put("ce-source", "value2");
        headers.put("ce-type", "value2");
        headers.put("ce-specversion", "value2");
        headers.put("ce-subject", "someSubject");

        Map<String, Object> body = new HashMap<>();
        body.put("name", "testfile.txt");
        String ErrMsg = RequestValidationUtility.validateRequest(body, headers);
        assertEquals(ErrMsg, "");
    }

    @Test
    public void testValidateRequestFailHeader1() {
        Map<String, String> headers = new HashMap<>();

        headers.put("ce-id", "value1");
        headers.put("ce-source", "value2");
        headers.put("ce-specversion", "value2");
        headers.put("ce-subject", "someSubject");

        Map<String, Object> body = new HashMap<>();
        body.put("name", "testfile.txt");
        String ErrMsg = RequestValidationUtility.validateRequest(body, headers);
        assertEquals(ErrMsg, "Missing expected header: ce-type.");
    }

    @Test
    public void testValidateRequestFailHeader2() {
        Map<String, String> headers = new HashMap<>();

        headers.put("ce-id", "value1");
        headers.put("ce-source", "value2");
        headers.put("ce-type", "value2");
        headers.put("ce-specversion", "value2");

        Map<String, Object> body = new HashMap<>();
        body.put("name", "testfile.txt");
        String ErrMsg = RequestValidationUtility.validateRequest(body, headers);
        assertEquals(ErrMsg, "Missing expected header: ce-subject.");
    }

    @Test
    public void testValidateRequestFailFileName() {
        Map<String, String> headers = new HashMap<>();
        headers.put("ce-id", "value1");
        headers.put("ce-source", "value2");
        headers.put("ce-type", "value2");
        headers.put("ce-subject", "value2");
        headers.put("ce-specversion", "value2");

        Map<String, Object> body = new HashMap<>();
        String ErrMsg = RequestValidationUtility.validateRequest(body, headers);
        assertEquals(ErrMsg, "Missing expected body element: file name");
    }
}
