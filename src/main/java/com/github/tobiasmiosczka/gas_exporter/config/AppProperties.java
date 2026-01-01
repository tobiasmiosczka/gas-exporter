package com.github.tobiasmiosczka.gas_exporter.config;

import com.github.tobiasmiosczka.gas_exporter.model.Position;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Set<Position> positions;

    public Set<Position> getPositions() {
        return positions;
    }

    public void setPositions(Set<Position> positions) {
        this.positions = positions;
    }
}
