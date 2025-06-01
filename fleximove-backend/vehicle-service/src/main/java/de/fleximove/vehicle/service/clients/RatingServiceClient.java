package de.fleximove.vehicle.service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ratingService")
public interface RatingServiceClient {

    @GetMapping("${ratingService.averageRatingForVehicle.endpoint}")
    Double getAverageRatingForVehicle(@PathVariable("vehicleId") Long vehicleId);

    @GetMapping("${ratingService.averageRatingForProvider.endpoint}")
    Double getAverageRatingForProvider(@PathVariable("providerId") Long providerId);
}
