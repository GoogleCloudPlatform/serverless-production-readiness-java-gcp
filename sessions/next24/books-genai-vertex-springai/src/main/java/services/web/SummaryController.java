package services.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import services.ai.VertexAIClient;
import services.domain.BooksService;
import services.domain.CloudStorageService;
import services.domain.FirestoreService;
import services.utility.FileUtility;
import services.utility.PromptUtility;
import services.utility.RequestValidationUtility;
import services.web.data.BookRequest;

import java.io.BufferedReader;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/summary")
public class SummaryController {

    private static final Logger logger = LoggerFactory.getLogger(SummaryController.class);

    private final BooksService booksService;
    private final VertexAIClient vertexAIClient;

    CloudStorageService cloudStorageService;

    public SummaryController(BooksService booksService, VertexAIClient vertexAIClient, CloudStorageService cloudStorageService) {
        this.booksService = booksService;
        this.vertexAIClient = vertexAIClient;
        this.cloudStorageService = cloudStorageService;
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> receiveMessage(
            @RequestBody Map<String, Object> body, @RequestHeader Map<String, String> headers) {
        String errorMsg = RequestValidationUtility.validateRequest(body,headers);
        if (!errorMsg.isBlank()) {
            return new ResponseEntity<>(errorMsg, HttpStatus.BAD_REQUEST);
        }

        // get document name and bucket
        String fileName = (String) body.get("name");
        String bucketName = (String) body.get("bucket");
        BufferedReader br = cloudStorageService.readFile(bucketName, fileName);
        long start = System.currentTimeMillis();
        logger.info("Book summary flow : start");

        String summmary = booksService.createBookSummary(br, fileName);

        logger.info("Book summary flow end " + (System.currentTimeMillis() - start) + "ms");

        // return the response to the caller
        return new ResponseEntity<>(summmary, HttpStatus.OK);
    }
}
