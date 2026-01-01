package com.github.tobiasmiosczka.gas_exporter;

import com.github.tobiasmiosczka.gas_exporter.config.ApiProperties;
import com.github.tobiasmiosczka.gas_exporter.model.Position;
import com.github.tobiasmiosczka.gas_exporter.model.dto.Station;
import com.github.tobiasmiosczka.gas_exporter.model.dto.StationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Set;
import java.util.function.Function;

@Component
public class PriceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceClient.class);

    public static final double MAX_RADIUS = 25.0D;
    public static final String PATH_STATION_LIST = "/list.php";

    private final WebClient client;

    public PriceClient(ApiProperties apiProperties) {
        this.client = WebClient.builder()
                .baseUrl(apiProperties.getUrl())
                .filter((request, next) -> {
                    URI newUri = UriComponentsBuilder.fromUri(request.url())
                            .queryParam("apikey", apiProperties.getApiKey())
                            .build(true)
                            .toUri();
                    ClientRequest newRequest = ClientRequest.from(request)
                            .url(newUri)
                            .build();
                    return next.exchange(newRequest);
                })
                .build();
    }

    public Flux<Station> loadStations(Set<Position> positions) {
        return this.loadStations(positions, MAX_RADIUS);
    }

    public Flux<Station> loadStations(Set<Position> positions, double radius) {
        return Flux.fromIterable(positions)
                .flatMap(position -> client.get()
                        .uri(buildUri(position, radius))
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(StationResponse.class)
                        .flatMapMany(response -> {
                            if (response.getStations() == null) {
                                return Flux.empty();
                            }
                            return Flux.fromIterable(response.getStations());
                        })
                )
                .distinct(Station::getId);
    }

    private Function<UriBuilder, URI> buildUri(Position position, double radius) {
        double finalRadius = Math.min(radius, MAX_RADIUS);
        return uriBuilder -> uriBuilder
                .path(PATH_STATION_LIST)
                .queryParam("lat", String.valueOf(position.latitude()))
                .queryParam("lng", String.valueOf(position.longitude()))
                .queryParam("rad", String.valueOf(finalRadius))
                .queryParam("type", "all")
                .queryParam("sort", "dist")
                .build();
    }
}