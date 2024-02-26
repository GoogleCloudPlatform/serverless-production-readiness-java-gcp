package services.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
//@ComponentScan("com.journaldev.spring")
public class DataAccess {

    JdbcTemplate jdbcTemplate;

    // Inject HikariDataSource as a bean dependency
    @Autowired
    public DataAccess(DataSource hikariDataSource) {
        jdbcTemplate = new JdbcTemplate(hikariDataSource);
    }

    public Map<String, Object> findBook(String prompt) {
        // Query the database
        // prompt = Give me the poems about love?
        String sql = "select\n" +
                "*\n" +
                "from\n" +
                "    books where title = ?";
        Object[] parameters = new Object[]{prompt};
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parameters);

        // Iterate over the results
        for (Map<String, Object> row : rows) {
            System.out.println(row.get("title"));
        }
        return rows.size()==0 ? new HashMap<>() : rows.get(0);
    }

    public List<Map<String, Object>> findPages(Integer bookId) {
        // Query the database
        // prompt = Give me the poems about love?
        String sql = "select\n" +
                "*\n" +
                "from\n" +
                "    pages where book_id = ? limit 10";
        Object[] parameters = new Object[]{bookId};
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parameters);

        // Iterate over the results
//        for (Map<String, Object> row : rows) {
            System.out.println("number of rows: " + rows.size());
//        }
        return rows;
    }


    // Perform database operations using the JdbcTemplate
    public List<Map<String, Object>> promptForBooks(String prompt) {
        // Query the database
        // prompt = Give me the poems about love?
        String sql = "SELECT\n" +
                "        b.title,\n" +
                "        left(p.content,500) as page,\n" +
                "        a.name,\n" +
                "        p.page_number,\n" +
                "        (p.embedding <=> embedding('textembedding-gecko@003', ?)::vector) as distance\n" +
                "FROM\n" +
                "        pages p\n" +
                "JOIN books b on\n" +
                "        p.book_id=b.book_id\n" +
                "JOIN authors a on\n" +
                "       a.author_id=b.author_id\n" +
                "ORDER BY\n" +
                "        distance ASC\n" +
                "LIMIT 10;";
//        String prompt ="yanni tests";
//        String prompt ="What kind of fruit trees grow well here?";
        Object[] parameters = new Object[]{prompt};
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parameters);

        // Iterate over the results
        for (Map<String, Object> row : rows) {
            System.out.println(row.get("page"));
        }
        return rows;
        // Insert data into the database
//        sql = "INSERT INTO table_name (column1, column2, column3) VALUES (?, ?, ?)";
//        jdbcTemplate.update(sql, "value1", "value2", "value3");
    }

    public Integer insert(Integer bookId, String content, Integer pageNumber) {
        String sql = "insert into pages (\n" +
                "book_id,\n" +
                "content,\n" +
                "    page_number)\n" +
                "values (?,?,?)";
        Object[] parameters = new Object[]{bookId, content, pageNumber};
        int success =jdbcTemplate.update(sql, parameters);
        return success;
    }
}