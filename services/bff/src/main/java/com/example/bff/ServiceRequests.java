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
package com.example.bff;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ServiceRequests {
    static ResponseBody makeAuthenticatedRequest(OkHttpClient ok, String url, String path) throws IOException {
        ResponseBody respBody = null;
        String target = String.format("%s/%s", url, path);

        // get ID token
        String token = getToken(target);

        // Instantiate HTTP request
        Request request =
            new Request.Builder()
                .url(target)
                .addHeader("Authorization", "Bearer " + token)
                .build();
    
        Response response = ok.newCall(request).execute();
        return response.body();
      }

    static ResponseBody makeAuthenticatedPostRequest(OkHttpClient ok, String url, String path, String data) throws IOException {
        ResponseBody respBody = null;
        String target = String.format("%s/%s", url, path);

        // get ID token
        String token = getToken(target);

        // Instantiate HTTP request
        MediaType contentType = MediaType.get("application/json; charset=utf-8");
        okhttp3.RequestBody body = okhttp3.RequestBody.create(data, contentType);
        Request request =
            new Request.Builder()
                .url(target)
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();
    
        Response response = ok.newCall(request).execute();
        return respBody = response.body();
      }

    static Response makeAuthenticatedDeleteRequest(OkHttpClient ok, String url, String path) throws IOException {
        ResponseBody respBody = null;
        String target = String.format("%s/%s", url, path);

        // Retrieve Application Default Credentials
        String token = getToken(target);

        Request request =
            new Request.Builder()
                .url(target)
                .addHeader("Authorization", "Bearer " + token)
                .delete()
                .build();
    
        Response response = ok.newCall(request).execute();
        return response;
      }

    private static String getToken(String target) throws IOException {
        // Retrieve Application Default Credentials
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        IdTokenCredentials tokenCredentials =
                IdTokenCredentials.newBuilder()
                        .setIdTokenProvider((IdTokenProvider) credentials)
                        .setTargetAudience(target)
                        .build();

        // Create an ID token
        return tokenCredentials.refreshAccessToken().getTokenValue();
    }
}
