package com.fleximove.rating.repository;

import com.fleximove.rating.model.RatingProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RatingProviderRepository extends JpaRepository<RatingProvider, UUID> {

    @Query("SELECT AVG(r.score) FROM RatingProvider r WHERE r.providerId = :providerId")
    Optional<Double> findAverageScoreByProviderId(UUID providerId);
}