package com.github.tobiasmiosczka.gas_exporter.config;

import com.github.tobiasmiosczka.gas_exporter.model.Position;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "app")
public record AppProperties (
        Set<Position> positions,
        String storageAccessKey,
        String storageSecret,
        String storageEndpoint,
        String storageBucket
) {}
