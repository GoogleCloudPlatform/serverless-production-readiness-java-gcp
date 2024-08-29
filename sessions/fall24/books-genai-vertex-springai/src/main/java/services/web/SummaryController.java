package services.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import services.actuator.StartupCheck;
import services.client.BooksService;
import services.utility.RequestValidationUtility;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Map;

/*
    Controller for creating summaries for documents stored in Google Cloud Storage

    Note: Current summarization pattern in use - prompt stuffing
    Alternatives - Map-reduce or prompt refining
 */
@RestController
@RequestMapping("/summary")
public class SummaryController {

    private static final Logger logger = LoggerFactory.getLogger(SummaryController.class);

    private final BooksService booksService;

    public SummaryController(BooksService booksService) {
        this.booksService = booksService;
    }

    @PostConstruct
    public void init() {
        logger.info("BookImagesApplication: SummaryController Post Construct Initializer {}",
                new SimpleDateFormat("HH:mm:ss.SSS").format(
                        new java.util.Date(System.currentTimeMillis())));
        logger.info(
                "BookImagesApplication: SummaryController Post Construct - StartupCheck can be enabled");

        StartupCheck.up();
    }

    @CrossOrigin
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

        // create a book summary and persist it in the database
        long start = System.currentTimeMillis();
        logger.info("Book summarization flow : start");
        logger.info("Summarize book with title {} from Cloud Storage bucket {}", fileName, bucketName);
        String summmary = booksService.createBookSummary(bucketName, fileName, true);
        logger.info("Book summarization flow: end {}ms", System.currentTimeMillis() - start);

        // return the response to the caller
        return new ResponseEntity<>(summmary, HttpStatus.OK);
    }
}
