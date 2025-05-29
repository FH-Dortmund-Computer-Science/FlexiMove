package de.fleximove.vehicle.service.services;

import de.fleximove.vehicle.service.clients.RatingServiceClient;
import de.fleximove.vehicle.service.clients.UserServiceClient;
import de.fleximove.vehicle.service.domain.Vehicle;
import de.fleximove.vehicle.service.domain.valueobject.*;
import de.fleximove.vehicle.service.dto.EditVehicleRequest;
import de.fleximove.vehicle.service.dto.NearestAvailableVehicleResponse;
import de.fleximove.vehicle.service.dto.VehicleRequest;
import de.fleximove.vehicle.service.dto.VehicleWithDistance;
import de.fleximove.vehicle.service.repository.VehicleRepository;
import de.fleximove.vehicle.service.utils.DistanceUtils;
import de.fleximove.vehicle.service.utils.VehicleMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final GeocodingService geocodingService;
    private final UserServiceClient userServiceClient;
    private final RatingServiceClient ratingServiceClient;

    @Autowired
    VehicleService(VehicleRepository vehicleRepository, VehicleMapper vehicleMapper, GeocodingService geocodingService, UserServiceClient userServiceClient, RatingServiceClient ratingServiceClient){
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
        this.geocodingService = geocodingService;
        this.userServiceClient = userServiceClient;
        this.ratingServiceClient = ratingServiceClient;
    }

    public void registerNewVehicle(VehicleRequest request, Long providerId) {
        Vehicle vehicle = vehicleMapper.mapToVehicle(request, providerId);
        vehicleRepository.save(vehicle);
    }

    public Optional<Vehicle> fetchVehicleById(Long id) {
        return vehicleRepository.findById(id);
    }

    public void deleteVehicle(Long id) {
        Vehicle vehicle = fetchVehicleById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle with ID " + id + " not found"));

        vehicleRepository.delete(vehicle);
    }

    public void deleteAllVehiclesByProviderId(Long providerId) {
        vehicleRepository.deleteAllByProviderId(providerId);
    }

    public List<NearestAvailableVehicleResponse> findAvailableNearbyVehicles(Location neededLocationWithAvailableVehicles, double radiusInKm) {
        Stream<VehicleWithDistance> foundAvailableVehicles = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).stream()
                .map(vehicle -> {
                    Location vahicleLocation = vehicle.getCurrentLocation();
                    double distance = DistanceUtils.haversineDistance(
                            neededLocationWithAvailableVehicles.getLatitude(), neededLocationWithAvailableVehicles.getLongitude(),
                            vahicleLocation.getLatitude(), vahicleLocation.getLongitude()
                    );
                    return new VehicleWithDistance(vehicle, distance);
                });

        return foundAvailableVehicles.filter(vehicleWithDistance -> vehicleWithDistance.distanceInKm() <= radiusInKm).map(vehicleWithDistance -> {
            Vehicle vehicle = vehicleWithDistance.vehicle();
            Double averageRatingForVehicle = ratingServiceClient.getAverageRatingForVehicle(vehicle.getId());
            Double averageRatingForProvider = ratingServiceClient.getAverageRatingForProvider(vehicle.getProviderId());
            String providerName = userServiceClient.getProviderCompanyName(vehicle.getProviderId());
            String address = geocodingService.reverseGeocode(vehicle.getCurrentLocation());
            return new NearestAvailableVehicleResponse(
                    vehicle.getId(),
                    vehicle.getVehicleModel(),
                    providerName,
                    vehicle.getVehicleType().toString(),
                    vehicle.getStatus().toString(),
                    vehicle.getVehiclePrice().getAmount(),
                    vehicle.getVehiclePrice().getBillingModel().toString(),
                    address,
                    vehicle.getCurrentLocation().getLatitude(),
                    vehicle.getCurrentLocation().getLongitude(),
                    vehicleWithDistance.distanceInKm(),
                    averageRatingForVehicle,
                    averageRatingForProvider
                    );
        }).toList();
    }

    public List<Vehicle> listVehiclesByProvider(Long providerId){
        return vehicleRepository.findAllByProviderId(providerId);
    }

    public void updateVehicleStatus(Long vehicleId, VehicleStatus newStatus) {
        Vehicle vehicle = fetchVehicleById(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found with ID: " + vehicleId));
        vehicle.setStatus(newStatus);
    }


    public void updateVehicleLocation(Long vehicleId, double latitude, double longitude) {
        Vehicle vehicle = fetchVehicleById(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found with ID: " + vehicleId));
        vehicle.setCurrentLocation(new Location(latitude, longitude));
        vehicleRepository.save(vehicle);
    }

    public void editVehicleInformation(Long vehicleId, EditVehicleRequest request) {
        Vehicle vehicleToUpdate = fetchVehicleById(vehicleId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle not found with ID: " + vehicleId));

        updateIdentificationNumberIfPresent(vehicleToUpdate, request);
        updateVehicleModelIfPresent(vehicleToUpdate, request);
        updateVehicleTypeIfPresent(vehicleToUpdate, request);
        updateRestrictionsIfPresent(vehicleToUpdate, request);
        updatePriceIfPresent(vehicleToUpdate, request);

        validateVehicleConsistency(vehicleToUpdate);

        vehicleRepository.save(vehicleToUpdate);
    }

    private void updateIdentificationNumberIfPresent(Vehicle vehicle, EditVehicleRequest request) {
        if (request.getIdentificationNumber() != null && !request.getIdentificationNumber().equals(vehicle.getIdentificationNumber().getIdentNumber())) {
            String newIdent = request.getIdentificationNumber();
            Optional<Vehicle> existingVehicleWithIdentNumber = vehicleRepository.findByIdentificationNumberIdentNumber(newIdent);
            if (existingVehicleWithIdentNumber.isPresent()) {
                throw new IllegalArgumentException("Identification number already in use.");
            }

            vehicle.setIdentificationNumber(new IdentificationNumber(newIdent));
        }
    }


    private void updateVehicleModelIfPresent(Vehicle vehicle, EditVehicleRequest request) {
        if (request.getVehicleModel() != null) {
            vehicle.setVehicleModel(request.getVehicleModel());
        }
    }

    private void updateVehicleTypeIfPresent(Vehicle vehicle, EditVehicleRequest request) {
        if (request.getVehicleType() != null) {
            VehicleType type = VehicleType.valueOf(request.getVehicleType().toUpperCase());
            vehicle.setVehicleType(type);
        }
    }

    private void updatePriceIfPresent(Vehicle vehicle, EditVehicleRequest request) {
        BillingModel billingModel = BillingModel.valueOf(request.getBillingModel().toUpperCase());
        vehicle.setVehiclePrice(new Price(request.getPriceAmount(), billingModel));
    }

    private void updateRestrictionsIfPresent(Vehicle vehicle, EditVehicleRequest request) {
        if (request.getMinAge() != null || request.getMaxBookingTimeMinutes() != null ||
                request.getMaxDistanceKm() != null || request.getMaxPassengers() != null ||
                request.getRequiredLicenseType() != null) {

            VehicleRestrictions restrictions = vehicle.getRestrictions();

            if (request.getMinAge() != null) { restrictions.setMinAge(request.getMinAge()); }
            if (request.getMaxBookingTimeMinutes() != null) { restrictions.setMaxBookingTimeMinutes(request.getMaxBookingTimeMinutes()); }
            if (request.getMaxDistanceKm() != null) { restrictions.setMaxDistanceKm(request.getMaxDistanceKm()); }
            if (request.getMaxPassengers() != null) { restrictions.setMaxPassengers(request.getMaxPassengers()); }

            if (request.getRequiredLicenseType() != null) {
                restrictions.setRequiredLicense(
                        DriverLicenseType.licenseTypeFromCode(request.getRequiredLicenseType())
                );
            }

            vehicle.setRestrictions(restrictions);
        }
    }

    private void validateVehicleConsistency(Vehicle vehicle) {
        VehicleRestrictions vehicleRestrictions = vehicle.getRestrictions();
        if (vehicle.getVehicleType().requiresLicense()) {
            DriverLicenseType license = vehicleRestrictions.getRequiredLicense();
            if (license == null || license == DriverLicenseType.NONE) {
                throw new IllegalStateException("Vehicle type '" + vehicle.getVehicleType() + "' requires a valid license, but 'NONE' was provided.");
            }
        }
        if(vehicle.getVehicleType().requiresMinAge() && vehicleRestrictions.getMinAge() == null) {
            throw new IllegalArgumentException("Minimum age is required for vehicle type: " + vehicle.getVehicleType());
        }
    }
}
