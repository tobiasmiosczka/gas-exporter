package com.github.tobiasmiosczka.gas_exporter.service.impl;

import com.github.tobiasmiosczka.gas_exporter.config.AppProperties;
import com.github.tobiasmiosczka.gas_exporter.model.dto.Station;
import com.github.tobiasmiosczka.gas_exporter.service.GasDataService;
import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class GasDataServiceImpl implements GasDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GasDataServiceImpl.class);

    private final AppProperties properties;

    private static final String CREATE_TABLE = """
            CREATE TABLE prices (
                time         TIMESTAMPTZ NOT NULL,
                station_id   UUID NOT NULL,
                name         VARCHAR,
                brand        VARCHAR,
                latitude     DOUBLE,
                longitude    DOUBLE,
                post_code    VARCHAR,
                place        VARCHAR,
                street       VARCHAR,
                house_number VARCHAR,
                is_open      BOOLEAN,
                e5           DECIMAL(5,3),
                e10          DECIMAL(5,3),
                diesel       DECIMAL(5,3),
                year         INTEGER NOT NULL,
                month        INTEGER NOT NULL,
                day          INTEGER NOT NULL
            )""";

    public GasDataServiceImpl(AppProperties appProperties) {
        this.properties = appProperties;
    }

    public Mono<Void> processStations(Flux<Station> stations, Instant time) {
        return stations.bufferTimeout(1000, Duration.ofMinutes(1))
                .concatMap(batch -> processBatch(batch, time))
                .then();
    }

    private Mono<Void> processBatch(List<Station> batch, Instant time) {
        return Mono.<Void>fromCallable(() -> {
                    LOGGER.info("Processing batch of {} stations", batch.size());
                    writeParquet(batch, time);
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void writeParquet(List<Station> batch, Instant time) throws SQLException {
        OffsetDateTime timestamp = time.atOffset(ZoneOffset.UTC);
        LocalDate date = timestamp.toLocalDate();
        try (DuckDBConnection connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(initS3());
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(CREATE_TABLE);
            }
            try (DuckDBAppender appender = connection.createAppender(DuckDBConnection.DEFAULT_SCHEMA, "prices")) {
                for (Station s : batch) {
                    appender.beginRow()
                            .append(timestamp)
                            .append(s.getId())
                            .append(s.getName())
                            .append(s.getBrand())
                            .append(s.getLat())
                            .append(s.getLng())
                            .append(s.getPostCode() == null ? null : String.valueOf(s.getPostCode()))
                            .append(s.getPlace())
                            .append(s.getStreet())
                            .append(s.getHouseNumber())
                            .append(s.isOpen())
                            .append(s.getE5())
                            .append(s.getE10())
                            .append(s.getDiesel())
                            .append(date.getYear())
                            .append(date.getMonthValue())
                            .append(date.getDayOfMonth())
                            .endRow();
                }
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        COPY prices TO 's3://%s/' (
                            FORMAT parquet,
                            PARTITION_BY (year, month, day),
                            APPEND
                        )
                        """.formatted(properties.storageBucket()));
            }
        }
    }

    private String initS3() {
        return """
                INSTALL httpfs;
                LOAD httpfs;
                
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
                properties.storageAccessKey(),
                properties.storageSecret(),
                properties.storageEndpoint());
    }
}
