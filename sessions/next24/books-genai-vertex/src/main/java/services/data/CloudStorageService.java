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
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
@Service
public class CloudStorageService {
    public BufferedReader readFile(String bucket, String fileName) {
        // Create a Storage client.
        Storage storage = StorageOptions.getDefaultInstance().getService();

        // Get the blob.
        Blob blob = storage.get(BlobId.of(bucket, fileName));

        ReadableByteChannel channel = blob.reader();
        InputStreamReader isr = new InputStreamReader(Channels.newInputStream(channel));
        BufferedReader br = new BufferedReader(isr);
        // Create a byte array input stream.
//        byte[] bytes = blob.getContent();
//        InputStream input = new ByteArrayInputStream(bytes);
//        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input));
        // Do something with the byte array.
        // System.out.println(new String(bytes));
        return br;
    }
}
