package com.tmc.aac.configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

@Configuration
public class InfrastructureConfiguration {
    @Bean
    Clock clock() { return Clock.systemUTC(); }

    @Bean
    DataSource dataSource(@Value("${edge.database}") String file) throws java.io.IOException {
        Path path = Path.of(file).toAbsolutePath();
        Files.createDirectories(path.getParent());
        SQLiteConfig config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.FULL);
        config.enforceForeignKeys(true);
        config.setBusyTimeout(5000);
        SQLiteDataSource source = new SQLiteDataSource(config);
        source.setUrl("jdbc:sqlite:" + path);
        return source;
    }

    @Bean
    TransactionTemplate transactionTemplate(DataSource source) {
        return new TransactionTemplate(new DataSourceTransactionManager(source));
    }
}
