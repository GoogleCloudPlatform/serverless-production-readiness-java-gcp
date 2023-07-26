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
package com.example.audit.domain;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteResult;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Audit Domain service to persistent Quote Audit Data
 */
@Service
public class AuditService {

  private final FirestoreOptions firestoreOptions;
  private final Firestore firestore;

  public AuditService() {
    this.firestoreOptions = FirestoreOptions.getDefaultInstance();
    this.firestore = firestoreOptions.getService();
  }
  public AuditService(FirestoreOptions firestoreOptions, Firestore firestore) {
    this.firestoreOptions = firestoreOptions;
    this.firestore = firestore;
  }

  public AuditService(Firestore firestore){
    this.firestoreOptions = null;
    this.firestore = firestore;
  }

  public ApiFuture<WriteResult> auditQuote(String quote, String author, String book, String randomID) {
    DocumentReference doc = firestore.collection("books").document(author);

    Map<String, Object> data = new HashMap<>();
    data.put("created", new Date());
    data.put("quote",quote);
    data.put("author",author);
    data.put("book",book);
    data.put("randomID",randomID);

    return doc.set(data, SetOptions.merge());
  }

}
