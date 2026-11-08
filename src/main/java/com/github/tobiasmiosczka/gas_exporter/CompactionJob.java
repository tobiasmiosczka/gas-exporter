package com.github.tobiasmiosczka.gas_exporter;

import com.github.tobiasmiosczka.gas_exporter.service.CompactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class CompactionJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompactionJob.class);

    private final CompactionService compactionService;

    private final int lookbackDays;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public CompactionJob(
            CompactionService compactionService,
            @Value("${app.compaction-lookback-days:7}") int lookbackDays) {
        this.compactionService = compactionService;
        this.lookbackDays = lookbackDays;
    }

    // Partitions are UTC dates; only days before today (UTC) are complete and safe to compact.

    // On startup, every existing partition before today is checked, so that a backlog is cleaned up.
    @EventListener(ApplicationReadyEvent.class)
    public void compactAllOnStartup() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        run("startup", compactionService.listPartitionDates()
                .filter(date -> date.isBefore(today)));
    }

    // Older days are included so that partitions missed by a failed run are caught up.
    @Scheduled(cron = "${app.compaction-schedule:0 0 1 * * *}")
    public void compact() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        run("nightly", Flux.range(1, lookbackDays)
                .map(today::minusDays));
    }

    private void run(String trigger, Flux<LocalDate> dates) {
        if (!running.compareAndSet(false, true)) {
            LOGGER.warn("Previous compaction is still in progress, skipping {} run", trigger);
            return;
        }
        LOGGER.info("Starting {} compaction", trigger);
        dates.concatMap(date -> compactionService.compactDay(date)
                        .doOnError(e -> LOGGER.error("Compaction of {} failed: ", date, e))
                        .onErrorComplete())
                .doFinally(signal -> running.set(false))
                .subscribe(
                        null,
                        e -> LOGGER.error("{} compaction failed: ", trigger, e),
                        () -> LOGGER.info("{} compaction finished", trigger));
    }

}
