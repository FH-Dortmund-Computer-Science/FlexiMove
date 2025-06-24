package com.fleximove.rating.service;

import com.fleximove.rating.model.RatingProvider;
import com.fleximove.rating.model.RatingVehicle;
import com.fleximove.rating.repository.RatingProviderRepository;
import com.fleximove.rating.repository.RatingVehicleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RatingService {

    private final RatingVehicleRepository ratingVehicleRepository;

    private final RatingProviderRepository ratingProviderRepository;

    @Autowired
    RatingService(RatingVehicleRepository ratingVehicleRepository, RatingProviderRepository ratingProviderRepository){
        this.ratingVehicleRepository = ratingVehicleRepository;
        this.ratingProviderRepository = ratingProviderRepository;
    }

    public RatingVehicle rateVehicle(RatingVehicle rating) {
        Optional<RatingVehicle> existing = ratingVehicleRepository.findByUserIdAndVehicleId(rating.getUserId(), rating.getVehicleId());

        if (existing.isPresent()) {
            RatingVehicle existingRating = existing.get();
            existingRating.setScore(rating.getScore());
            existingRating.setComment(rating.getComment());
            existingRating.setTimestamp(LocalDateTime.now());
            return ratingVehicleRepository.save(existingRating);
        } else {
            return ratingVehicleRepository.save(rating);
        }
    }


    public RatingProvider rateProvider(RatingProvider rating) {
        Optional<RatingProvider> existing = ratingProviderRepository.findByUserIdAndProviderId(rating.getUserId(), rating.getProviderId());

        if (existing.isPresent()) {
            RatingProvider existingRating = existing.get();
            existingRating.setScore(rating.getScore());
            existingRating.setComment(rating.getComment());
            existingRating.setTimestamp(LocalDateTime.now());
            return ratingProviderRepository.save(existingRating);
        } else {
            return ratingProviderRepository.save(rating);
        }
    }

    public double getAverageRatingForVehicle(Long vehicleId) {
        Double avgVehicleRating;
        try {
            avgVehicleRating = ratingVehicleRepository.findAverageScoreByVehicleId(vehicleId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No ratings found for this vehicle."));
        } catch (Exception e) {
            avgVehicleRating = 0.0;
        }
        return avgVehicleRating;
    }

    public double getAverageRatingForProvider(Long providerId) {
        Double avgProviderRating;
        try {
            avgProviderRating = ratingProviderRepository.findAverageScoreByProviderId(providerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No ratings found for this provider."));
        } catch (Exception e) {
            avgProviderRating = 0.0;
        }
        return avgProviderRating;
    }

    @Transactional
    public void deleteAllCustomerRatings(Long userId) {
        List<RatingProvider> ratedProvider = ratingProviderRepository.findByUserId(userId);
        List<RatingVehicle> ratedVehicles = ratingVehicleRepository.findByUserId(userId);

        if (!ratedProvider.isEmpty()) {
            ratingProviderRepository.deleteByUserId(userId);
        }

        if (!ratedVehicles.isEmpty()) {
            ratingVehicleRepository.deleteByUserId(userId);
        }
    }

    public void deleteAllProviderRatings(Long providerId) {
        List<RatingProvider> ratings = ratingProviderRepository.findByProviderId(providerId);
        if (!ratings.isEmpty()) {
            ratingProviderRepository.deleteByUserId(providerId);
        }
    }

    public void deleteAllVehicleRatings(Long vehicleId) {
        List<RatingVehicle> ratings = ratingVehicleRepository.findByVehicleId(vehicleId);
        if (!ratings.isEmpty()) {
            ratingVehicleRepository.deleteByVehicleId(vehicleId);
        }
    }
}