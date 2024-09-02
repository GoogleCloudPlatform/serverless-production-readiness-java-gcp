package services.web;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import services.actuator.StartupCheck;
import services.client.BooksService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.text.SimpleDateFormat;

/*
    Controller for uploading documents (for embedding or images for analysis) to Google Cloud Storage
 */
@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    private final BooksService booksService;

    public UploadController(BooksService booksService) {
        this.booksService = booksService;
    }

    @PostConstruct
    public void init() {
        logger.info("BookImagesApplication: UploadController Post Construct Initializer {}",
                new SimpleDateFormat("HH:mm:ss.SSS").format(
                        new java.util.Date(System.currentTimeMillis())));
        logger.info(
                "BookImagesApplication: UploadController Post Construct - StartupCheck can be enabled");

        StartupCheck.up();
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<String> receiveMessage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bucketName") String bucketName) {

        // create a book summary and persist it in the database
        long start = System.currentTimeMillis();
        logger.info("Book summarization flow : start");
        logger.info("Upload image to Cloud Storage bucket {}", bucketName);

        try {
            // Read the file content
            byte[] fileContent = file.getBytes();

            // Create a unique file name
            String fileName = file.getOriginalFilename();

            // Create a BlobId
            BlobId blobId = BlobId.of(bucketName, fileName);

            // Create BlobInfo
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();

            // Create a Storage client.
            Storage storage = StorageOptions.getDefaultInstance().getService();
            // Upload the file to Google Cloud Storage
            storage.create(blobInfo, fileContent);
            logger.info("File uploaded successfully: {} to bucket: {}", fileName, bucketName);

            // If it's an image, you might want to process it or create a summary
            if (file.getContentType() != null && file.getContentType().startsWith("image/")) {
                // Here you could call a method to process the image or create a summary
                // For example: String summary = booksService.createImageSummary(bucketName, fileName);
                // return new ResponseEntity<>(summary, HttpStatus.OK);
            }

            //String summary = booksService.createBookSummary(bucketName, fileName, true);
            logger.info("Upload file: end {}ms", System.currentTimeMillis() - start);

            // return the response to the caller
            return new ResponseEntity<>("File uploaded successfully: " + fileName, HttpStatus.OK);
        } catch (IOException e) {
            logger.error("Failed to upload file", e);
            return new ResponseEntity<>("Failed to upload file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
