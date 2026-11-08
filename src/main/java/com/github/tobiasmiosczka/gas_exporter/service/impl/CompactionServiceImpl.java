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
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CompactionServiceImpl implements CompactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompactionServiceImpl.class);

    private static final Pattern PARTITION_PATTERN = Pattern.compile("^year=(\\d+)/month=(\\d+)/day=(\\d+)/$");

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

    // Walks the partition directories via delimiter listings instead of listing every file in the bucket,
    // so the number of requests depends on the number of days, not on the number of files.
    public Flux<LocalDate> listPartitionDates() {
        return Mono.fromCallable(() -> listPrefixes("year=").stream()
                        .flatMap(year -> listPrefixes(year + "month=").stream())
                        .flatMap(month -> listPrefixes(month + "day=").stream())
                        .map(PARTITION_PATTERN::matcher)
                        .filter(Matcher::matches)
                        .map(matcher -> LocalDate.of(
                                Integer.parseInt(matcher.group(1)),
                                Integer.parseInt(matcher.group(2)),
                                Integer.parseInt(matcher.group(3))))
                        .sorted()
                        .toList())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapIterable(dates -> dates);
    }

    private void compact(LocalDate date) throws SQLException {
        String partition = "year=%d/month=%d/day=%d/".formatted(
                date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        List<String> keys = listParquetKeys(partition);
        if (keys.size() <= 1) {
            LOGGER.info("Partition {} has {} file(s), nothing to compact", partition, keys.size());
            return;
        }
        String target = toS3Url(partition + "compacted_" + UUID.randomUUID() + ".parquet");
        LOGGER.info("Compacting {} files of partition {} into {}", keys.size(), partition, target);
        try (DuckDBConnection connection = connectionFactory.openWithS3();
             Statement statement = connection.createStatement()) {
            // DISTINCT removes duplicates left behind by a previous run that failed after writing
            statement.execute("""
                    COPY (
                        SELECT DISTINCT *
                        FROM read_parquet([%s], hive_partitioning = false)
                        ORDER BY station_id, time
                    ) TO '%s' (FORMAT parquet)
                    """.formatted(toSqlList(keys), target));
        }
        for (String key : keys) {
            s3Client.deleteObject(request -> request
                    .bucket(properties.storageBucket())
                    .key(key));
        }
        LOGGER.info("Compacted {} files of partition {} into {}", keys.size(), partition, target);
    }

    private List<String> listPrefixes(String prefix) {
        return s3Client.listObjectsV2Paginator(request -> request
                        .bucket(properties.storageBucket())
                        .prefix(prefix)
                        .delimiter("/"))
                .commonPrefixes()
                .stream()
                .map(CommonPrefix::prefix)
                .toList();
    }

    private List<String> listParquetKeys(String prefix) {
        return s3Client.listObjectsV2Paginator(request -> request
                        .bucket(properties.storageBucket())
                        .prefix(prefix))
                .contents()
                .stream()
                .map(S3Object::key)
                .filter(key -> key.endsWith(".parquet"))
                .toList();
    }

    private String toS3Url(String key) {
        return "s3://%s/%s".formatted(properties.storageBucket(), key);
    }

    private String toSqlList(List<String> keys) {
        return keys.stream()
                .map(key -> "'" + toS3Url(key).replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
    }
}
