/*
 * Copyright 2024 Google LLC
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
package services.utility;

public class FileUtility {

    public static String getTitle (String fileName) {
        String [] fileNameArray = fileName.split("-");
        return fileNameArray[0];
    }

    public static String getAuthor(String fileName) {
        String [] fileNameArray = fileName.split("-");
        return fileNameArray[1];
    }

    public static String getYear(String fileName) {
        String [] fileNameArray = fileName.split("-");
        return fileNameArray[2]+"-01"+"-01";
    }
// write a unit test to test the FileUtility class
    public static String getPublicPrivate(String fileName) {
        String [] fileNameArray = fileName.split("-");
        String publicPrivate = fileNameArray[3];
        if(publicPrivate.contains(".txt")){
            publicPrivate = publicPrivate.replaceAll("\\.txt$", "");
        }
        return publicPrivate;
    }

}
