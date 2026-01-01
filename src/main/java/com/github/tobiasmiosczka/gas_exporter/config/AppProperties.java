package com.github.tobiasmiosczka.gas_exporter.config;

import com.github.tobiasmiosczka.gas_exporter.model.Position;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AppProperties {


    private Set<Position> positions;

    public Set<Position> getPositions() {
        return positions;
    }

    public void setPositions(Set<Position> positions) {
        this.positions = positions;
    }
}
