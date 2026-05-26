package com.mem0.core.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * DataSource configuration for MySQL history store.
 * The primary DataSource (PostgreSQL) is auto-configured by Spring Boot
 * from spring.datasource.* properties. This creates a secondary MySQL
 * DataSource specifically for the history store.
 *
 * @author MoBai

 */
@Configuration
public class HistoryDataSourceConfig {

    /**
     * Creates a MySQL DataSource for the history store.
     * Configured from mem0.history.mysql.* properties.
     * Not marked @Primary so it won't interfere with the auto-configured
     * PostgreSQL DataSource used by JPA.
     */
    @Bean("historyDataSource")
    @ConfigurationProperties(prefix = "mem0.history.postgresql")
    public DataSource historyDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }
}
