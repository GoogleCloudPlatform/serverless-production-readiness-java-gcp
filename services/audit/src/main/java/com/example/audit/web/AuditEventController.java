/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.audit.web;

import com.example.audit.actuator.StartupCheck;
import com.example.audit.domain.AuditService;
import com.google.api.core.ApiFuture;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.cloud.firestore.WriteResult;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to handle Audit data persist requests
 */
@RestController
public class AuditEventController {
    private static final Logger logger = LoggerFactory.getLogger(AuditEventController.class);

    private static final List<String> requiredFields = Arrays.asList("ce-id", "ce-source", "ce-type", "ce-specversion");

    @Autowired
    private AuditService eventService;

    @PostConstruct
    public void init() {
        logger.info("AuditApplication: AuditController Post Construct Initializer " + new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
        logger.info("AuditApplication: AuditController Post Construct - StartupCheck can be enabled");

        StartupCheck.up();
    }

    @GetMapping("start")
    String start() {
        logger.info("AuditApplication: AuditController - Executed start endpoint request " + new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
        return "AuditController started";
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<String> receiveMessage(
            @RequestBody Map<String, Object> body, @RequestHeader Map<String, String> headers) throws IOException, InterruptedException, ExecutionException {

        // Validate the number of available processors
        logger.info("EventController: Active processors: " + Runtime.getRuntime().availableProcessors());

        System.out.println("Header elements");
        for (String field : requiredFields) {
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

        String msg = "OK";

        Map<String, String> message = (Map<String, String>) body.get("message");
        String quote = message.get("quote");
        String author = message.get("author");
        String book = message.get("book");
        String randomID = message.get("randomId");

        // Saving result to Firestore
        try {
            ApiFuture<WriteResult> writeResult = eventService.auditQuote(quote, author, book, randomID);
            msg = String.format("Book metadata saved in Firestore at %s",
                writeResult.get().getUpdateTime());
            logger.info(msg);
        } catch(IllegalArgumentException e) {
            System.out.println("Could not write quote data to Firestore" + e.getMessage());
            return new ResponseEntity<String>(msg, HttpStatus.FAILED_DEPENDENCY);
        } catch(PermissionDeniedException | StatusRuntimeException e){
            String.format("Can not access the Firestore service - permission denied: %s", e.getMessage());
            System.out.println(msg); 
            return new ResponseEntity<String>(msg, HttpStatus.UNAUTHORIZED);
        } catch(Exception e) {
            msg = String.format("Can not access the Firestore service: %s", e.getMessage());
            System.out.println(msg);
            return new ResponseEntity<String>(msg, HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<String>(msg, HttpStatus.OK);
    }
}
