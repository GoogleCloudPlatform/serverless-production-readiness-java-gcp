package services.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import services.config.CloudConfig;

import java.util.Map;

public class RequestValidationUtility {

    private static final Logger logger = LoggerFactory.getLogger(RequestValidationUtility.class);

    public static String validateRequest(Map<String, Object> body, Map<String, String> headers) {
        String errorMsg="";
        logger.info("Header elements");
        for (String field : CloudConfig.requiredFields) {
            if (headers.get(field) == null) {
                errorMsg = String.format("Missing expected header: %s.", field);
                logger.info(errorMsg);
            } else {
                logger.info(field + " : " + headers.get(field));
            }
        }

        logger.info("Body elements");
        for (String bodyField : body.keySet()) {
            logger.info(bodyField + " : " + body.get(bodyField));
        }

        if (headers.get("ce-subject") == null) {
            errorMsg = "Missing expected header: ce-subject.";
            logger.error(errorMsg);
        }

        String ceSubject = headers.get("ce-subject");
        errorMsg = "Detected change in Cloud Storage bucket: (ce-subject) : " + ceSubject;
        logger.info(errorMsg);

        String fileName = (String)body.get("name");
        if(fileName == null){
            errorMsg = "Missing expected body element: file name";
            logger.error(errorMsg);
        }
        return errorMsg;
    }
}
