/*
    * Copyright 2021 Google LLC
    *
    * Licensed under the Apache License, Version 2.0 (the "License");
    * you may not use this file except in compliance with the License.
    * You may obtain a copy of the License at
    *
    * http://www.apache.org/licenses/LICENSE-2.0
    *
    * Unless required by applicable law or agreed to in writing, software
    * distributed under the License is distributed on an "AS IS" BASIS,
    * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    * See the License for the specific language governing permissions and
    * limitations under the License.
    */
package services.data;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteResult;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FirestoreService {

  private final FirestoreOptions firestoreOptions;
  private final Firestore firestore;

  public FirestoreService() {
    this.firestoreOptions = FirestoreOptions.getDefaultInstance();
    this.firestore = firestoreOptions.getService();
  }
  public FirestoreService(FirestoreOptions firestoreOptions, Firestore firestore) {
    this.firestoreOptions = firestoreOptions;
    this.firestore = firestore;
  }

  public ApiFuture<WriteResult> storeImage(String fileName, List<String> labels, String mainColor, String modelResponse) {
    DocumentReference doc = firestore.collection("pictures").document(fileName);

    Map<String, Object> data = new HashMap<>();
    data.put("labels", labels);
    data.put("color", mainColor);
    data.put("created", new Date());
    data.put("modelResponse", modelResponse);

    return doc.set(data, SetOptions.merge());
  }

}
