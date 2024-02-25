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

    public Integer insertPagesBook(String book) {
//        content = (content==null || content.equals("") ? "random yanni test" + Math.random(): content);
        Integer success = 0;
        if( book == null || book.equals("") ) {
            return success;
        }

        BufferedReader reader = null;
        System.out.println(book);
        try {
            ClassPathResource classPathResource = new ClassPathResource(book);
            InputStream inputStream = classPathResource.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String content;
            Integer page = 1;

            char[] cbuf = new char[6000];

            // Read 3000 characters at a time
            int charsRead;
            while ((charsRead = reader.read(cbuf)) != -1) {

                // Print the characters read
                content = new String(cbuf, 0, charsRead);
                dao.insert(10,content,page);
                page++;
//                if(page==2) {
//                    break;
//                }
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
