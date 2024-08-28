package services.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import services.ai.VertexAIClient;
import services.domain.BooksService;
import services.domain.CloudStorageService;
import services.utility.RequestValidationUtility;

import java.util.Map;

@RestController
@RequestMapping("/summary")
public class SummaryController {

    private static final Logger logger = LoggerFactory.getLogger(SummaryController.class);

    private final BooksService booksService;
    CloudStorageService cloudStorageService;

    public SummaryController(BooksService booksService, CloudStorageService cloudStorageService) {
        this.booksService = booksService;
        this.cloudStorageService = cloudStorageService;
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> receiveMessage(
            @RequestBody Map<String, Object> body, @RequestHeader Map<String, String> headers) {
        String errorMsg = RequestValidationUtility.validateRequest(body);
        if (!errorMsg.isBlank()) {
            return new ResponseEntity<>(errorMsg, HttpStatus.BAD_REQUEST);
        }

        // get document name and bucket
        String fileName = (String) body.get("name");
        String bucketName = (String) body.get("bucket");

        long start = System.currentTimeMillis();
        logger.info("Book summarization flow : start");
        logger.info("Summarize book with title {} from Cloud Storage bucket {}", fileName, bucketName);
        String summmary = booksService.createBookSummary(bucketName, fileName, true);
        logger.info("Book summarization flow: end {}ms", System.currentTimeMillis() - start);

        // return the response to the caller
        return new ResponseEntity<>(summmary, HttpStatus.OK);
    }
}
