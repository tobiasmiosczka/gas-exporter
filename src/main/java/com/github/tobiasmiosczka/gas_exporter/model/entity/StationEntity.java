package com.github.tobiasmiosczka.gas_exporter.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("stations")
public record StationEntity(
        @Id UUID id,
        String name,
        String brand,
        Double latitude,
        Double longitude,
        String postCode,
        String place,
        String street,
        String houseNumber
) {
}