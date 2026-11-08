package com.github.tobiasmiosczka.gas_exporter.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface CompactionService {

    Mono<Void> compactDay(LocalDate date);

    Flux<LocalDate> listPartitionDates();

}
