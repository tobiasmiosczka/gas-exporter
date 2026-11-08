package com.github.tobiasmiosczka.gas_exporter.service.impl;

import com.github.tobiasmiosczka.gas_exporter.config.AppProperties;
import org.duckdb.DuckDBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class DuckDbConnectionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DuckDbConnectionFactory.class);

    public static final int MAX_S3_RETRIES = 2;

    private final AppProperties properties;

    public DuckDbConnectionFactory(AppProperties appProperties) {
        this.properties = appProperties;
    }

    public DuckDBConnection openWithS3() throws SQLException {
        DuckDBConnection connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:");
        try (Statement statement = connection.createStatement()) {
            statement.execute(initS3());
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        LOGGER.info("Initialized S3 connection to {}", properties.storageEndpoint());
        return connection;
    }

    private String initS3() {
        return """
                INSTALL httpfs;
                LOAD httpfs;
                SET http_retries = %d;

                CREATE SECRET garage (
                    TYPE s3,
                    KEY_ID '%s',
                    SECRET '%s',
                    ENDPOINT '%s',
                    REGION 'garage',
                    URL_STYLE 'path',
                    USE_SSL false
                );
                """.formatted(
                MAX_S3_RETRIES,
                properties.storageAccessKey(),
                properties.storageSecret(),
                properties.storageEndpoint());
    }
}
