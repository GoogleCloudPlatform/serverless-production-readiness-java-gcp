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

import domain.ScopeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import services.data.dao.DataAccess;
import services.web.data.BookRequest;
import utility.FileUtility;
import utility.PromptUtility;
import utility.SqlUtility;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class BooksService {
    @Autowired
    DataAccess dao;

    @Autowired
    CloudStorageService cloudStorageService;
    public List<Map<String, Object>>  prompt(String prompt) {
        return dao.promptForBooks(prompt, 0);
    }

    public List<Map<String, Object>>  prompt(String prompt, Integer characterLimit) {
        return dao.promptForBooks(prompt, characterLimit);
    }

    public List<Map<String, Object>>  prompt(BookRequest bookRequest, Integer characterLimit) {
        String prompt = PromptUtility.formatPromptBookKeywords(bookRequest.keyWords());
        return dao.promptForBooks(prompt, bookRequest.book(), bookRequest.author(), characterLimit);
    }

    public Integer insertBook(String fileName) {
        String author = FileUtility.getAuthor(fileName);
        author = SqlUtility.replaceUnderscoresWithSpaces(author);
        String title = FileUtility.getTitle(fileName);
        title = SqlUtility.replaceUnderscoresWithSpaces(title);
        String year = FileUtility.getYear(fileName);
        String publicPrivate = FileUtility.getPublicPrivate(fileName);
        Map<String, Object> book = dao.findBook(title);
        Map<String, Object> authorMap = dao.findAuthor(author);
        Object authorId = authorMap.get("author_id");
        Integer bookId = 0;
        if(!book.isEmpty()){
            bookId = (Integer) book.get("book_id");
        } else {
            if(authorId==null)
                authorId = dao.insertAuthor("famous author", author);
            System.out.println("publicPrivate:"+publicPrivate);
            bookId = dao.insertBook( (Integer) authorId, title, year, ScopeType.fromValue(publicPrivate));
        }

        return bookId;
    }

    public String getBookSummary(String bookTitle) {
        Map<String, Object> book = dao.findBook(bookTitle);
        Map<String, Object> summary = new HashMap<>();
        if(!book.isEmpty()){
            Integer bookId = (Integer) book.get("book_id");
            summary = dao.findSummaries(bookId);
        }
        return summary.isEmpty() ? "" : (String) summary.get("summary");
    }

    public Integer insertPagesBook(String filePath, String bookTitle) {
        //0 = failure, 1 = success
        Integer success = 0;
        if( filePath == null || filePath.equals("") || bookTitle==null || bookTitle.equals("")) {
            return success;
        }

        Map<String, Object> book = dao.findBook(bookTitle);

        if(book.isEmpty()){
            return success;
        }

        BufferedReader reader = null;
        Integer bookId = (Integer) book.get("book_id");
        System.out.println(filePath+" "+bookTitle+" bookId:"+bookId);
        try {
            //replace with cloud storage eventually
            ClassPathResource classPathResource = new ClassPathResource(filePath);
            InputStream inputStream = classPathResource.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            success = insertPagesBook(reader, bookTitle);
        }catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return success;
    }

    public Integer insertPagesBook(BufferedReader reader, String bookTitle) {
        Integer success = 0;
        Map<String, Object> book = dao.findBook(bookTitle);

        if(book.isEmpty()){
            return success;
        }
        Integer bookId = (Integer) book.get("book_id");
        System.out.println("bookId:"+bookId);
        try {
            String content;
            Integer page = 1;
            List<Map<String, Object>> pages = dao.findPages(bookId);
            if(!pages.isEmpty()) {
                return success;
            }
            char[] cbuf = new char[6000];
            // Read 3000 characters at a time
            int charsRead;
            while ((charsRead = reader.read(cbuf)) != -1) {
                content = new String(cbuf, 0, charsRead);
                dao.insertPages( bookId,content,page );
                page++;
            }
            reader.close();
            success=1;
        }catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return success;

    }


}
