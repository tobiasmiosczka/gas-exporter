package com.github.tobiasmiosczka.gas_exporter.repository;

import com.github.tobiasmiosczka.gas_exporter.model.entity.StationEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;


import java.util.UUID;

@Repository
public interface StationRepository extends ReactiveCrudRepository<StationEntity, UUID> {

    @Modifying
    @Query("""
        INSERT INTO stations (id, name, brand, latitude, longitude, post_code, place, street, house_number)
        VALUES (:#{#e.id}, :#{#e.name}, :#{#e.brand}, :#{#e.latitude}, :#{#e.longitude}, :#{#e.postCode}, :#{#e.place}, :#{#e.street}, :#{#e.houseNumber})
        ON CONFLICT (id) DO UPDATE SET
            name = EXCLUDED.name,
            brand = EXCLUDED.brand,
            latitude = EXCLUDED.latitude,
            longitude = EXCLUDED.longitude
        """)
    Mono<Void> upsert(@Param("e") StationEntity entity);

}