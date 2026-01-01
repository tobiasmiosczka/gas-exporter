package com.github.tobiasmiosczka.gas_exporter.service.impl;

import com.github.tobiasmiosczka.gas_exporter.model.dto.Station;
import com.github.tobiasmiosczka.gas_exporter.model.entity.PriceEntity;
import com.github.tobiasmiosczka.gas_exporter.model.entity.StationEntity;
import com.github.tobiasmiosczka.gas_exporter.repository.PriceRepository;
import com.github.tobiasmiosczka.gas_exporter.repository.StationRepository;
import com.github.tobiasmiosczka.gas_exporter.service.GasDataService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class GasDataServiceImpl implements GasDataService {

    private final StationRepository stationRepository;
    private final PriceRepository priceRepository;

    public GasDataServiceImpl(
            StationRepository stationRepository,
            PriceRepository priceRepository) {
        this.stationRepository = stationRepository;
        this.priceRepository = priceRepository;
    }

    public Mono<Void> processStations(Flux<Station> stations, Instant time) {
        return stations.concatMap(s -> stationRepository.upsert(convert(s))
                .onErrorResume(DataIntegrityViolationException.class, e -> Mono.empty())
                .thenMany(savePrice(s, time))
                .then(), 10).then();
    }

    private static StationEntity convert(Station s) {
        return new StationEntity(
                s.getId(),
                s.getName(),
                s.getBrand(),
                s.getLat(),
                s.getLng(),
                String.valueOf(s.getPostCode()),
                s.getPlace(),
                s.getStreet(),
                s.getHouseNumber());
    }

    private Flux<PriceEntity> savePrice(Station s, Instant time) {
        return priceRepository.saveAll(List.of(
                new PriceEntity(time, s.getId(), "e5", s.getE5()),
                new PriceEntity(time, s.getId(), "e10", s.getE10()),
                new PriceEntity(time, s.getId(), "diesel", s.getDiesel())));
    }
}
