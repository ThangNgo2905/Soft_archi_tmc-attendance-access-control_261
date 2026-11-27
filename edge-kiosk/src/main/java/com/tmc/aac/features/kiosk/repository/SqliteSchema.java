package com.tmc.aac.features.kiosk.repository;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** The independent Edge uses versioned SQLite SQL; central PostgreSQL Flyway migrations stay separate. */
@Component
public class SqliteSchema {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    public SqliteSchema(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc; this.transactions = transactions;
    }
    @PostConstruct
    void migrate() {
        transactions.executeWithoutResult(status -> {
            jdbc.execute("CREATE TABLE IF NOT EXISTS edge_schema_version(version INTEGER PRIMARY KEY)");
            Integer version = jdbc.queryForObject("SELECT COALESCE(MAX(version),0) FROM edge_schema_version", Integer.class);
            if (version != null && version > 1) throw new IllegalStateException("Unsupported Edge database version");
            if (version == null || version < 1) {
                var source = java.util.Objects.requireNonNull(jdbc.getDataSource());
                var connection = DataSourceUtils.getConnection(source);
                try { ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V1__edge.sql")); }
                finally { DataSourceUtils.releaseConnection(connection, source); }
                jdbc.update("INSERT INTO edge_schema_version(version) VALUES(1)");
            }
        });
    }
}
