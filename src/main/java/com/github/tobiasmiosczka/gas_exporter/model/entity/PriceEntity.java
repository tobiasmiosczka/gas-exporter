package com.github.tobiasmiosczka.gas_exporter.model.entity;


import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("prices")
public record PriceEntity(
        @Column("time") Instant time,
        @Column("station_id") UUID stationId,
        String name,
        BigDecimal price
) {}