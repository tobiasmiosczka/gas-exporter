package com.github.tobiasmiosczka.gas_exporter.repository;

import com.github.tobiasmiosczka.gas_exporter.model.entity.PriceEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface PriceRepository extends ReactiveCrudRepository<PriceEntity, UUID> {
}
