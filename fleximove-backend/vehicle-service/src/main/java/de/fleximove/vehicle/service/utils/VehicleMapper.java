package de.fleximove.vehicle.service.utils;

import de.fleximove.vehicle.service.domain.Vehicle;
import de.fleximove.vehicle.service.domain.valueobject.*;
import de.fleximove.vehicle.service.dto.VehicleRequest;
import de.fleximove.vehicle.service.services.GeocodingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class VehicleMapper {

    GeocodingService geocodingService;

    @Autowired
    VehicleMapper(GeocodingService geocodingService){
        this.geocodingService = geocodingService;
    }

    public Vehicle mapToVehicle(VehicleRequest request, Long providerId) {
        VehicleType type = VehicleType.valueOf(request.getVehicleType().toUpperCase());
        BillingModel billingModel = BillingModel.valueOf(request.getBillingModel().toUpperCase());
        DriverLicenseType licenseType = resolveLicenseType(type, request.getRequiredLicenseType());
        Location currentLocation = resolveAddress(request.getAddress());

        if (type.requiresMinAge() && request.getMinAge() == null) {
            throw new IllegalArgumentException("Minimum age is required for vehicle type: " + type);
        }

        return new Vehicle(
                new IdentificationNumber(request.getIdentificationNumber()),
                request.getVehicleName(),
                type,
                currentLocation,
                new Price(request.getPriceAmount(), billingModel),
                new VehicleRestrictions(
                        request.getMinAge(),
                        request.getMaxBookingTimeMinutes(),
                        request.getMaxDistanceKm(),
                        request.getMaxPassengers(),
                        licenseType
                ),
                providerId
        );
    }

    private static DriverLicenseType resolveLicenseType(VehicleType type, String licenseCodeFromRequest) {
        if (licenseCodeFromRequest == null) {
            throw new IllegalArgumentException("License type is required for vehicle type: " + type);
        }

        DriverLicenseType licenseTypeFromRequest = DriverLicenseType.licenseTypeFromCode(licenseCodeFromRequest);

        if(type.requiresLicense() && licenseTypeFromRequest == DriverLicenseType.NONE){
            throw new IllegalArgumentException("A valid driver license is required for vehicle type '" + type + "', but 'NONE' was provided.");
        }
        return licenseTypeFromRequest;
    }

    private Location resolveAddress(String address){
        Objects.requireNonNull(address);
        return geocodingService.geocodeAddress(address);
    }
}

