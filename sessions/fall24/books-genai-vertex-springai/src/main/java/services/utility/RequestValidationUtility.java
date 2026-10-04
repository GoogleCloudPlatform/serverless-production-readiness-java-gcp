package services.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.config.CloudConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class RequestValidationUtility {

    private static final Logger logger = LoggerFactory.getLogger(RequestValidationUtility.class);

    private static final Set<String> DEFAULT_ALLOWED_BUCKETS = Set.of(
            "library_public",
            "library_private",
            "libarary_public",
            "libarary_private",
            "library_images",
            "library_next24_images",
            "library_summary"
    );

    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._ -]+$");

    public static Set<String> getAllowedBuckets() {
        Set<String> allowed = new HashSet<>(DEFAULT_ALLOWED_BUCKETS);
        String envBuckets = System.getenv("ALLOWED_BUCKETS");
        if (envBuckets != null && !envBuckets.isBlank()) {
            Arrays.stream(envBuckets.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(allowed::add);
        }
        return allowed;
    }

    public static boolean isValidBucket(String bucket) {
        if (bucket == null || bucket.isBlank()) {
            return false;
        }
        return getAllowedBuckets().contains(bucket);
    }

    public static boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return false;
        }
        return SAFE_FILENAME_PATTERN.matcher(fileName).matches();
    }

    public static boolean isValidBookFileName(String fileName) {
        if (!isValidFileName(fileName)) {
            return false;
        }
        String[] parts = fileName.split("-");
        if (parts.length != 4) {
            return false;
        }
        String scope = parts[3].contains(".")
                ? parts[3].replaceAll("\\.[^.]+$", "")
                : parts[3];
        return "public".equalsIgnoreCase(scope) || "private".equalsIgnoreCase(scope);
    }

    public static String validateRequest(Map<String, Object> body, Map<String, String> headers) {
        String errorMsg="";
        logger.info("Header elements");
        for (String field : CloudConfig.requiredFields) {
            if (headers.get(field) == null) {
                errorMsg = String.format("Missing expected header: %s.", field);
                logger.info(errorMsg);
                return errorMsg;
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
            return errorMsg;
        }

        String ceSubject = headers.get("ce-subject");
        logger.info("Detected change in Cloud Storage bucket: (ce-subject) : " + ceSubject);

        Object nameObj = body.get("name");
        if (!(nameObj instanceof String fileName)) {
            errorMsg = "Missing expected body element: file name";
            logger.error(errorMsg);
            return errorMsg;
        }
        if (!isValidFileName(fileName)) {
            errorMsg = "Invalid body element: file name";
            logger.error(errorMsg);
            return errorMsg;
        }

        if (body.containsKey("bucket")) {
            Object bucketObj = body.get("bucket");
            if (!(bucketObj instanceof String bucket) || !isValidBucket(bucket)) {
                errorMsg = "Invalid or unauthorized bucket.";
                logger.error(errorMsg);
                return errorMsg;
            }
        }
        return "";
    }

    public static String validateRequest(Map<String, Object> body) {
        String errorMsg="";
        logger.info("Body elements");
        for (String bodyField : body.keySet()) {
            logger.info(bodyField + " : " + body.get(bodyField));
        }

        Object nameObj = body.get("name");
        if (!(nameObj instanceof String fileName)) {
            errorMsg = "Missing expected body element: file name";
            logger.error(errorMsg);
            return errorMsg;
        }
        if (!isValidFileName(fileName)) {
            errorMsg = "Invalid body element: file name";
            logger.error(errorMsg);
            return errorMsg;
        }

        if (body.containsKey("bucket")) {
            Object bucketObj = body.get("bucket");
            if (!(bucketObj instanceof String bucket) || !isValidBucket(bucket)) {
                errorMsg = "Invalid or unauthorized bucket.";
                logger.error(errorMsg);
                return errorMsg;
            }
        }
        return "";
    }
}
