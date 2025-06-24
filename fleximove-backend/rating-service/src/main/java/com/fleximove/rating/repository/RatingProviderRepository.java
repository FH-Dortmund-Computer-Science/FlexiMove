package com.fleximove.rating.repository;

import com.fleximove.rating.model.RatingProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RatingProviderRepository extends JpaRepository<RatingProvider, Long> {

    @Query("SELECT AVG(r.score) FROM RatingProvider r WHERE r.providerId = :providerId")
    Optional<Double> findAverageScoreByProviderId(Long providerId);

    void deleteByUserId(Long userId);
    List<RatingProvider> findByProviderId(Long providerId);
    List<RatingProvider> findByUserId(Long userId);
    Optional<RatingProvider> findByUserIdAndProviderId(Long userId, Long providerId);
}