package services.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import services.actuator.StartupCheck;
import services.client.BooksService;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Map;

/*
    Controller for classifying a book, analyzing sentiment and getting additional recommendations

 */
@RestController
@RequestMapping("/sentiment")
public class SentimentAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(SentimentAnalysisController.class);

    private final BooksService booksService;

    public SentimentAnalysisController(BooksService booksService) {
        this.booksService = booksService;
    }

    @PostConstruct
    public void init() {
        logger.info("BookImagesApplication: SentimentAnalysisController Post Construct Initializer {}",
                new SimpleDateFormat("HH:mm:ss.SSS").format(
                        new java.util.Date(System.currentTimeMillis())));
        logger.info(
                "BookImagesApplication: SentimentAnalysisController Post Construct - StartupCheck can be enabled");

        StartupCheck.up();
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> receiveMessageWebSearch(
            @RequestBody Map<String, Object> body, @RequestHeader Map<String, String> headers) {

        // get document name and bucket
        String title = (String) body.get("title");
        String author = (String) body.get("author");

        // create a book summary and persist it in the database
        long start = System.currentTimeMillis();
        logger.info("Sentiment analysis flow : start");
        logger.info("Categorize and analyze sentiment in the book with title {} and author {}", title, author);
        String response = booksService.sentimentAnalysis(title, author);
        logger.info("Sentiment analysis flow: end {}ms", System.currentTimeMillis() - start);

        // return the response to the caller
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
