package de.fleximove.vehicle.service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ratingService", path = "${ratingService.base-path}")
public interface RatingServiceClient {

    @GetMapping("/vehicle/{vehicleId}/average")
    Double getAverageRatingForVehicle(@PathVariable("vehicleId") Long vehicleId);

    @GetMapping("/provider/{providerId}/average")
    Double getAverageRatingForProvider(@PathVariable("providerId") Long providerId);
}
