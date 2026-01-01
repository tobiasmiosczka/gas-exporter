package com.github.tobiasmiosczka.gas_exporter;

import com.github.tobiasmiosczka.gas_exporter.config.AppProperties;
import com.github.tobiasmiosczka.gas_exporter.model.Position;
import com.github.tobiasmiosczka.gas_exporter.model.dto.Station;
import com.github.tobiasmiosczka.gas_exporter.service.GasDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.Set;

@Component
public class DataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);

    private final PriceClient priceClient;

    private final GasDataService gasDataService;

    private final Set<Position> positions;

    public DataLoader(
            PriceClient priceClient,
            AppProperties appProperties,
            GasDataService gasDataService) {
        this.priceClient = priceClient;
        this.positions = appProperties.getPositions();
        this.gasDataService = gasDataService;
    }

    @Scheduled(cron = "${app.schedule:0 0/10 * * * *}")
    public void loadData() {
        Instant time = Instant.now();
        Flux<Station> stationFlux = priceClient.loadStations(positions);
        gasDataService.processStations(stationFlux, time)
                .doOnSuccess(v -> LOGGER.info("Sucessfully loaded data for {}", positions))
                .doOnError(e -> LOGGER.error("Error: ", e))
                .subscribe();
    }

}
