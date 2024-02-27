package services.dao;

import domain.ScopeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
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

    public Map<String, Object> findBook(String title) {
        // Query the database
        // prompt = Give me the poems about love?
        String sql = "select\n" +
                "*\n" +
                "from\n" +
                "    books where title = ?";
        Object[] parameters = new Object[]{title};
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parameters);

        // Iterate over the results
        for (Map<String, Object> row : rows) {
            System.out.println(row.get("title"));
        }
        return rows.size()==0 ? new HashMap<>() : rows.get(0);
    }

    public Map<String, Object> findAuthor(String authorName) {
        // Query the database
        // prompt = Give me the poems about love?
        String sql = "select\n" +
                "*\n" +
                "from\n" +
                "    authors where name = ?";
        Object[] parameters = new Object[]{authorName};
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
    public List<Map<String, Object>> promptForBooks(String prompt, Integer characterLimit) {
        // Query the database
        // prompt = Give me the poems about love?
        if (characterLimit == null || characterLimit == 0) {
            characterLimit = 2000;
        }
        String sql = "SELECT\n" +
                "        b.title,\n" +
                "        left(p.content,?) as page,\n" +
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
        Object[] parameters = new Object[]{characterLimit, prompt};
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

    public Integer insertPages(Integer bookId, String content, Integer pageNumber) {
        String sql = "insert into pages (\n" +
                "book_id,\n" +
                "content,\n" +
                "    page_number)\n" +
                "values (?,?,?)";
        Object[] parameters = new Object[]{bookId, content, pageNumber};
        int success =jdbcTemplate.update(sql, parameters);
        return success;
    }

    public Integer insertAuthor(String bio, String author) {
//        String sql = "insert into authors (\n" +
//                "    \"bio\",\n" +
//                "    \"name\")\n" +
//                "values (?, ?)";
//        Object[] parameters = new Object[]{bio, author};
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("bio", bio);
        parameters.put("name", author);
        SimpleJdbcInsert insertIntoAuthors = new SimpleJdbcInsert(jdbcTemplate).withTableName("authors")
                .usingColumns("bio", "name")
                .usingGeneratedKeyColumns("author_id");
//        int success =jdbcTemplate.update(sql, parameters);
        Number authorId = insertIntoAuthors.executeAndReturnKey(parameters);

        return Integer.valueOf(authorId.intValue());
    }

    public Integer insertBook(Integer authorId, String title, String year, ScopeType publicPrivate) {
//        String sql = "INSERT INTO " + tableName + " (scope) VALUES (CAST(? AS scope_type))";
        LocalDate publicationYear = LocalDate.parse(year);
        String sql = "insert into books (\n" +
                "    author_id,\n" +
                "    publication_year,\n" +
                "    title, scope)\n" +
                "values (\n" +
                "    ?,\n" +
                "    ?,\n" +
                "    ?, CAST(? AS scope_type))\n";
        Object[] parameters = new Object[]{authorId, publicationYear, title, publicPrivate.getValue()};
//        final Map<String, Object> parameters = new HashMap<>();
//        parameters.put("author_id", authorId);
//        parameters.put("publication_year", year);
//        parameters.put("title", title);
//        parameters.put("scope", ScopeType.fromValue(publicPriavate));
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql,
                    Statement.RETURN_GENERATED_KEYS);
            // Set the parameters on the PreparedStatement
            for (int i = 0; i < parameters.length; i++) {
                ps.setObject(i + 1, parameters[i]);
            }

            return ps;
        }, keyHolder);
//        SimpleJdbcInsert insertIntoAuthors = new SimpleJdbcInsert(jdbcTemplate).withTableName("books").usingGeneratedKeyColumns("book_id");
//        int success =jdbcTemplate.update(sql, parameters);
//        Number bookId = insertIntoAuthors.executeAndReturnKey(parameters);
        return Integer.valueOf(keyHolder.getKeys().get("book_id")+"");
    }
}