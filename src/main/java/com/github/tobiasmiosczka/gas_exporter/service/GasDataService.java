package com.github.tobiasmiosczka.gas_exporter.service;

import com.github.tobiasmiosczka.gas_exporter.model.dto.Station;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface GasDataService {

    Mono<Void> processStations(Flux<Station> stations, Instant time);

}