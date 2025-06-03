package com.fleximove.rating.controller;

import com.fleximove.rating.model.RatingProvider;
import com.fleximove.rating.model.RatingVehicle;
import com.fleximove.rating.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/ratings")
public class RatingController {

    @Autowired
    private RatingService ratingService;

    @PostMapping("/vehicle")
    public ResponseEntity<RatingVehicle> rateVehicle(@RequestBody RatingVehicle rating) {
        return ResponseEntity.ok(ratingService.rateVehicle(rating));
    }

    @PostMapping("/provider")
    public ResponseEntity<RatingProvider> rateProvider(@RequestBody RatingProvider rating) {
        return ResponseEntity.ok(ratingService.rateProvider(rating));
    }

    @GetMapping("/vehicle/{vehicleId}/average")
    public ResponseEntity<Double> getAverageVehicleRating(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(ratingService.getAverageRatingForVehicle(vehicleId));
    }

    @GetMapping("/provider/{providerId}/average")
    public ResponseEntity<Double> getAverageProviderRating(@PathVariable UUID providerId) {
        return ResponseEntity.ok(ratingService.getAverageRatingForProvider(providerId));
    }
}