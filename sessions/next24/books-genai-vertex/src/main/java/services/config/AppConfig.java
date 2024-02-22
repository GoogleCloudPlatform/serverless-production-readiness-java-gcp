package services.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class AppConfig {

    @Bean
    public HikariDataSource getDataSource() {
        HikariConfig config = new HikariConfig();
        HikariDataSource ds;
        config.setJdbcUrl("jdbc:postgresql://34.121.92.253:5000/quickstart_db");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("databaseName", "quickstart_db");
        config.addDataSourceProperty("port", "5000");
        ds = new HikariDataSource(config);
        return ds;
    }

//    @Bean
//    RestTemplate restTemplate() {
//        RestTemplate restTemplate = new RestTemplate();
//        return restTemplate;
//    }

}