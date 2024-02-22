package services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.ColorInfo;
import com.google.cloud.vision.v1.DominantColorsAnnotation;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageProperties;
import com.google.cloud.vision.v1.ImageSource;
import com.google.cloud.vision.v1.Likelihood;
import com.google.cloud.vision.v1.Property;
import com.google.cloud.vision.v1.SafeSearchAnnotation;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.vertexai.VertexAiChatModel;
import dev.langchain4j.model.vertexai.VertexAiLanguageModel;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import services.actuator.StartupCheck;
@RestController
@RequestMapping("/document")
public class DocumentEmbeddingController {
  private static final Logger logger = LoggerFactory.getLogger(DocumentEmbeddingController.class);

  @Autowired
  TableService tableService;

  @PostConstruct
  public void init() {
    logger.info("BookImagesApplication: DocumentEmbeddingController Post Construct Initializer " + new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
    logger.info("BookImagesApplication: DocumentEmbeddingController Post Construct - StartupCheck can be enabled");

    StartupCheck.up();
  }

  @GetMapping("start")
  String start(){
    logger.info("BookImagesApplication: DocumentEmbeddingController - Executed start endpoint request " + new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
    return "DocumentEmbeddingController started";
  }

  @RequestMapping(value = "/embeddings", method = RequestMethod.GET)
  public ResponseEntity<List<Map<String, Object>>> getTable(@RequestParam(name = "prompt") String prompt) {
    return new ResponseEntity<List<Map<String, Object>>>(tableService.getTable(prompt), HttpStatus.OK);
  }

  @RequestMapping(value = "/insert", method = RequestMethod.POST)
  public ResponseEntity<Integer> insertTable(@RequestBody Map<String, Object> body) {
    return new ResponseEntity<Integer>(tableService.insertTable( (String) body.get("content")), HttpStatus.OK);
  }

  @RequestMapping(value = "/", method = RequestMethod.POST)
  public ResponseEntity<String> receiveMessage(
      @RequestBody Map<String, Object> body, @RequestHeader Map<String, String> headers) throws IOException, InterruptedException, ExecutionException {
    System.out.println("Header elements");
    for (String field : CloudConfig.requiredFields) {
      if (headers.get(field) == null) {
        String msg = String.format("Missing expected header: %s.", field);
        System.out.println(msg);
        return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
      } else {
        System.out.println(field + " : " + headers.get(field));
      }
    }

    System.out.println("Body elements");
    for (String bodyField : body.keySet()) {
      System.out.println(bodyField + " : " + body.get(bodyField));
    }

    if (headers.get("ce-subject") == null) {
      String msg = "Missing expected header: ce-subject.";
      System.out.println(msg);
      return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
    }

    String ceSubject = headers.get("ce-subject");
    String msg = "Detected change in Cloud Storage bucket: (ce-subject) : " + ceSubject;
    System.out.println(msg);

    String fileName = (String)body.get("name");
    String bucketName = (String)body.get("bucket");

    logger.info("New book uploaded for embedding" + fileName);

    if(fileName == null){
      msg = "Missing expected body element: file name";
      System.out.println(msg);
      return new ResponseEntity<String>(msg, HttpStatus.BAD_REQUEST);
    }

    // add embedding functionality here


    // success
    return new ResponseEntity<String>(msg, HttpStatus.OK);
  }
}
