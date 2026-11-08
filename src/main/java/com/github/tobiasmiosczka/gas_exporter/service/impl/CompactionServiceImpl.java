package com.github.tobiasmiosczka.gas_exporter.service.impl;

import com.github.tobiasmiosczka.gas_exporter.config.AppProperties;
import com.github.tobiasmiosczka.gas_exporter.service.CompactionService;
import org.duckdb.DuckDBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.s3.S3Client;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CompactionServiceImpl implements CompactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompactionServiceImpl.class);

    private static final Pattern PARTITION_PATTERN = Pattern.compile("/year=(\\d+)/month=(\\d+)/day=(\\d+)/");

    private final AppProperties properties;

    private final DuckDbConnectionFactory connectionFactory;

    private final S3Client s3Client;

    public CompactionServiceImpl(
            AppProperties appProperties,
            DuckDbConnectionFactory connectionFactory,
            S3Client s3Client) {
        this.properties = appProperties;
        this.connectionFactory = connectionFactory;
        this.s3Client = s3Client;
    }

    public Mono<Void> compactDay(LocalDate date) {
        return Mono.<Void>fromCallable(() -> {
                    compact(date);
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<LocalDate> listPartitionDates() {
        return Mono.fromCallable(() -> {
                    String bucketPrefix = "s3://%s/".formatted(properties.storageBucket());
                    try (DuckDBConnection connection = connectionFactory.openWithS3()) {
                        return listFiles(connection, bucketPrefix + "year=*/month=*/day=*/*.parquet");
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapIterable(files -> files.stream()
                        .map(PARTITION_PATTERN::matcher)
                        .filter(Matcher::find)
                        .map(matcher -> LocalDate.of(
                                Integer.parseInt(matcher.group(1)),
                                Integer.parseInt(matcher.group(2)),
                                Integer.parseInt(matcher.group(3))))
                        .collect(Collectors.toCollection(TreeSet::new)));
    }

    private void compact(LocalDate date) throws SQLException {
        String partition = "year=%d/month=%d/day=%d".formatted(
                date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        String bucketPrefix = "s3://%s/".formatted(properties.storageBucket());
        try (DuckDBConnection connection = connectionFactory.openWithS3()) {
            List<String> files = listFiles(connection, bucketPrefix + partition + "/*.parquet");
            if (files.size() <= 1) {
                LOGGER.info("Partition {} has {} file(s), nothing to compact", partition, files.size());
                return;
            }
            String target = bucketPrefix + partition + "/compacted_" + UUID.randomUUID() + ".parquet";
            LOGGER.info("Compacting {} files of partition {} into {}", files.size(), partition, target);
            // DISTINCT removes duplicates left behind by a previous run that failed after writing
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        COPY (
                            SELECT DISTINCT *
                            FROM read_parquet([%s], hive_partitioning = false)
                            ORDER BY station_id, time
                        ) TO '%s' (FORMAT parquet)
                        """.formatted(toSqlList(files), target));
            }
            for (String file : files) {
                s3Client.deleteObject(request -> request
                        .bucket(properties.storageBucket())
                        .key(file.substring(bucketPrefix.length())));
            }
            LOGGER.info("Compacted {} files of partition {} into {}", files.size(), partition, target);
        }
    }

    private List<String> listFiles(DuckDBConnection connection, String pattern) throws SQLException {
        List<String> files = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT file FROM glob('%s')".formatted(pattern))) {
            while (resultSet.next()) {
                files.add(resultSet.getString(1));
            }
        }
        return files;
    }

    private static String toSqlList(List<String> files) {
        return files.stream()
                .map(file -> "'" + file.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
    }
}
