package de.fleximove.vehicle.service.controller;

import de.fleximove.vehicle.service.domain.Vehicle;
import de.fleximove.vehicle.service.domain.valueobject.Location;
import de.fleximove.vehicle.service.domain.valueobject.VehicleStatus;
import de.fleximove.vehicle.service.dto.EditVehicleRequest;
import de.fleximove.vehicle.service.dto.NearestAvailableVehicleResponse;
import de.fleximove.vehicle.service.services.GeocodingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import de.fleximove.vehicle.service.services.VehicleService;
import de.fleximove.vehicle.service.dto.VehicleRequest;

import java.util.List;

//TODO: exception handling
//TODO: split controller
@RestController
@RequestMapping("/vehicles")
public class VehicleController {
    private final VehicleService vehicleService;
    private final GeocodingService geocodingService;

    @Autowired
    public VehicleController(VehicleService vehicleService, GeocodingService geocodingService) {
        this.vehicleService = vehicleService;
        this.geocodingService = geocodingService;
    }

    //Request kommt aus Frontend
    @PostMapping("/registeredBy")
    public ResponseEntity<Void> registerVehicle(@RequestBody VehicleRequest request, @RequestParam Long providerId) {
        vehicleService.registerNewVehicle(request, providerId);
        return ResponseEntity.ok().build();
    }

    //Request kommt aus Frontend oder aus RatingService oder aus BookingService
    @GetMapping("/load/{id}")
    public ResponseEntity<?> getVehicle(@PathVariable Long id) {
        try {
            return vehicleService.fetchVehicleById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Fehler beim Abrufen des Fahrzeugs: " + ex.getMessage());
        }
    }

    //Request kommt aus Frontend, wird von Provider getriggered
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok().build();
    }

    //Request kommt aus UserService
    @DeleteMapping("/deleteAllVehicles")
    public ResponseEntity<Void> deleteVehiclesByProvider(@RequestParam Long deleteForProviderId) {
        vehicleService.deleteAllVehiclesByProviderId(deleteForProviderId);
        return ResponseEntity.ok().build();
    }

    //Request kommt aus Frontend
    @GetMapping("/nearby")
    public ResponseEntity<List<NearestAvailableVehicleResponse>> getNearbyVehicles(
            @RequestParam String address,
            @RequestParam(required = false, defaultValue = "3.0") double radiusInKm) {
        Location neededLocation = geocodingService.geocodeAddress(address);
        List<NearestAvailableVehicleResponse> vehicles = vehicleService.findAvailableNearbyVehicles(neededLocation, radiusInKm);
        return ResponseEntity.ok(vehicles);
    }

    //Request kommt aus Frontend, wird beim Laden von Provider-Profile getriggered
    @GetMapping("/providerVehiclesList")
    public ResponseEntity<List<Vehicle>> listProviderVehicles(@RequestParam Long forProviderId) {
        List<Vehicle> vehicles = vehicleService.listVehiclesByProvider(forProviderId);
        return ResponseEntity.ok(vehicles);
    }

    //Request kommt aus Frontend, wird von Provider getriggered
    @PatchMapping("/edit/{vehicleId}")
    public ResponseEntity<Void> editVehicle(@PathVariable Long vehicleId, @RequestBody EditVehicleRequest request) {
        vehicleService.editVehicleInformation(vehicleId, request);
        return ResponseEntity.ok().build();
    }

    //Request kommt aus Frontend, wird von Provider oder waehrend Booking getriggered
    @PatchMapping("/updateStatus/{vehicleId}")
    public ResponseEntity<Void> updateVehicleStatus(@PathVariable Long vehicleId, @RequestParam VehicleStatus newStatus
    ) {
        vehicleService.updateVehicleStatus(vehicleId, newStatus);
        return ResponseEntity.ok().build();
    }

    //TODO: trigger patch-method refreshVehicleInformation on the bookingService
    @PatchMapping("/updateLocation/{vehicleId}")
    public ResponseEntity<Void> updateVehicleLocation(@PathVariable Long vehicleId, @RequestBody Location locationData) {
        vehicleService.updateVehicleLocation(
                vehicleId,
                locationData.getLatitude(),
                locationData.getLongitude()
        );
        return ResponseEntity.ok().build();
    }

    //TODO: look for vehicles with other status?
}
