package services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import services.dao.DataAccess;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
@Service
public class BooksService {
    @Autowired
    DataAccess dao;
    public List<Map<String, Object>>  prompt(String prompt) {
        return dao.promptForBooks(prompt);
    }

    public Integer insertPagesBook(String filePath, String bookTitle) {
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
            ClassPathResource classPathResource = new ClassPathResource(filePath);
            InputStream inputStream = classPathResource.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
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

                // Print the characters read
                content = new String(cbuf, 0, charsRead);

                dao.insert( bookId,content,page );
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
