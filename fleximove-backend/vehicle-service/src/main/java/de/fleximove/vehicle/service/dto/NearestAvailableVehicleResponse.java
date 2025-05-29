package de.fleximove.vehicle.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NearestAvailableVehicleResponse {
    private Long vehicleId ;
    private String vehicleName;
    private String providerName;
    private String vehicleType;
    private String status;
    private double priceAmount;
    private String billingModel;
    private String address;
    private double latitude;
    private double longitude;
    private double distanceInKm;
    private double averageVehicleRating;
    private double averageProviderRating;
}

