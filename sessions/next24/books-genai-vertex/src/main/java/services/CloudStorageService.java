package services;
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
