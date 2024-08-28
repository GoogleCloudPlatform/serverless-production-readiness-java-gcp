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
package services.domain.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.domain.util.ScopeType;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class DataAccess {

    JdbcTemplate jdbcTemplate;
    private Environment environment;
    private static final Logger logger = LoggerFactory.getLogger(DataAccess.class);

    @Autowired
    public DataAccess(Environment environment) {
        this.environment = environment;
        jdbcTemplate = new JdbcTemplate(getDataSource());
    }

    public HikariDataSource getDataSource() {
        HikariConfig config = new HikariConfig();
        HikariDataSource ds;
        config.setJdbcUrl(environment.getProperty("spring.datasource.url"));
        config.setUsername(environment.getProperty("spring.datasource.username"));
        config.setPassword(environment.getProperty("spring.datasource.password"));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("databaseName", "library");
        config.addDataSourceProperty("port", "5432");
        ds = new HikariDataSource(config);
        return ds;
    }

    public Map<String, Object> findBook(String title) {
        String sql = "select\n" +
                "*\n" +
                "from\n" +
                "    books where UPPER(title) = UPPER(?)";
        Object[] parameters = new Object[]{title};
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parameters);

        for (Map<String, Object> row : rows) {
            logger.info("Found book with title {}", row.get("title"));
        }
        return rows.size()==0 ? new HashMap<>() : rows.get(0);
    }

    public Map<String, Object> findAuthor(String authorName) {
        String sql = "select\n" +
                "*\n" +
                "from\n" +
                "    authors where name = ?";
        Object[] parameters = new Object[]{authorName};
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parameters);

        for (Map<String, Object> row : rows) {
            logger.info(row.get("name")+"");
        }
        return rows.size()==0 ? new HashMap<>() : rows.get(0);
    }

    public Map<String, Object> findSummaries(Integer bookId) {
        String sql = """
                select * from bookSummaries where book_id = ? limit 10
                """;
        Object[] parameters = new Object[]{bookId};
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parameters);

        logger.info("number of rows: " + rows.size());

        return rows.size()==0 ? new HashMap<>() : rows.get(0);
    }

    public List<Map<String, Object>> findPages(Integer bookId) {
        String sql = "select\n" +
                "*\n" +
                "from\n" +
                "    pages where book_id = ? limit 10";
        Object[] parameters = new Object[]{bookId};
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parameters);

        logger.info("number of rows: " + rows.size());
        return rows;
    }

    public List<Map<String, Object>> promptForBooks(String prompt, Integer characterLimit) {
        return promptForBooks(prompt, null, null, characterLimit);
    }

    public List<Map<String, Object>> promptForBooks(String prompt, String book, String author, Integer characterLimit) {
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
                "       a.author_id=b.author_id\n";
        Object[] parameters = new Object[]{characterLimit, prompt, book, author};
        List<Object> params = Arrays.stream(parameters)
                .filter(Objects::nonNull)
                .filter(p -> p instanceof String ? !((String) p).isEmpty() : true)
                .collect(Collectors.toList());
        logger.info(params.toString());
        if ( params.size()>2 ) {
            sql += createWhereClause(book, author);
        }
        sql += " ORDER BY\n" +
                "distance ASC\n" +
                "LIMIT 10;";
        logger.info(sql);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

        return rows;
    }

    private String createWhereClause(String book, String author) {
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("WHERE ");
        if (book != null) {
            whereClause.append("b.title = ?");
        }

        if (author != null) {
            if (whereClause.length() > 0) {
                whereClause.append(" AND ");
            }
            whereClause.append("a.name = ?");
        }

        return whereClause.toString();
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

    public Integer insertSummaries(Integer bookId, String summary) {
        String sql = "insert into bookSummaries (\n" +
                "book_id,\n" +
                "summary)\n" +
                "values (?,?)";
        Object[] parameters = new Object[]{bookId, summary};
        int success =jdbcTemplate.update(sql, parameters);
        return success;
    }

    public Integer insertAuthor(String bio, String author) {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("bio", bio);
        parameters.put("name", author);
        SimpleJdbcInsert insertIntoAuthors = new SimpleJdbcInsert(jdbcTemplate).withTableName("authors")
                .usingColumns("bio", "name")
                .usingGeneratedKeyColumns("author_id");
        Number authorId = insertIntoAuthors.executeAndReturnKey(parameters);

        return Integer.valueOf(authorId.intValue());
    }

    public Integer insertBook(Integer authorId, String title, String year, ScopeType publicPrivate) {
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
        return Integer.valueOf(keyHolder.getKeys().get("book_id")+"");
    }
}