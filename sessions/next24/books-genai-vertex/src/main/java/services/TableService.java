package services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import services.dao.DataAccess;

import java.util.List;
import java.util.Map;
@Service
public class TableService {
    @Autowired
    DataAccess dao;
    public List<Map<String, Object>>  getTable(String prompt) {
        return dao.queryTable(prompt);
    }

    public Integer insertTable(String content) {
        content = (content==null || content.equals("") ? "random yanni test" + Math.random(): content);
        return dao.insert(content);
    }


}
