package services.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
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

    // Perform database operations using the JdbcTemplate
    public List<Map<String, Object>> queryTable(String prompt) {
        // Query the database
        String sql = "SELECT\n" +
                "        cp.product_name,\n" +
                "        left(cp.product_description,80) as description,\n" +
                "        cp.sale_price,\n" +
                "        cs.zip_code,\n" +
                "        (cp.embedding <=> embedding('textembedding-gecko@003', ?)::vector) as distance\n" +
                "FROM\n" +
                "        cymbal_products cp\n" +
                "JOIN cymbal_inventory ci on\n" +
                "        ci.uniq_id=cp.uniq_id\n" +
                "JOIN cymbal_stores cs on\n" +
                "        cs.store_id=ci.store_id\n" +
                "        AND ci.inventory>0\n" +
                "        AND cs.store_id = 1583\n" +
                "ORDER BY\n" +
                "        distance ASC\n" +
                "LIMIT 10;\n";
//        String prompt ="yanni tests";
//        String prompt ="What kind of fruit trees grow well here?";
        Object[] parameters = new Object[]{prompt};
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, parameters);

        // Iterate over the results
        for (Map<String, Object> row : rows) {
            System.out.println(row.get("description"));
        }
        return rows;
        // Insert data into the database
//        sql = "INSERT INTO table_name (column1, column2, column3) VALUES (?, ?, ?)";
//        jdbcTemplate.update(sql, "value1", "value2", "value3");
    }

    public Integer insert(String content) {
        String sql = "INSERT INTO public.cymbal_products (product_url, product_name, product_description, list_price, sale_price, brand, item_number, gtin, package_size, category, postal_code, available) VALUES " +
                "('https://www.cymbalstore.com/ip/Disney-Frozen-2-Magical-Journey-Window-Valance-Walmart-Exclusive/1234', " +
                "'Disney Frozen 2 Magical Journey Window Valance, CymbalStore Exclusive', ?, 19.99, 14.99, 'Disney+', '578491190', '0085214126180', NULL, 'Home | Decor | Curtains & Window Treatments | Curtains | Kids Curtains'" +
                ", NULL, true);";
        int success =jdbcTemplate.update(sql, content);
        return success;
    }
}