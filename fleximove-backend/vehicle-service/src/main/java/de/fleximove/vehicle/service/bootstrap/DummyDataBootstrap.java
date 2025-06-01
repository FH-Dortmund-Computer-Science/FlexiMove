package de.fleximove.vehicle.service.bootstrap;

import de.fleximove.vehicle.service.domain.Vehicle;
import de.fleximove.vehicle.service.domain.valueobject.*;
import de.fleximove.vehicle.service.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class DummyDataBootstrap implements ApplicationListener<ContextRefreshedEvent> {

    private final VehicleRepository vehicleRepository;

    @Autowired
    public DummyDataBootstrap(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        initData();
    }

    private void initData() {
        vehicleRepository.deleteAll();
        IdentificationNumber identificationNumber1 = new IdentificationNumber("DEABC123");
        VehicleType vehicleType1 = VehicleType.CAR;
        Location vehicleLocation1 = new Location(51.517883350000005,7.486873003317484);
        Price vehiclePrice1 = new Price(4, BillingModel.PER_KILOMETER);
        VehicleRestrictions restrictions1 = new VehicleRestrictions(18, 180, 200.0, 4, DriverLicenseType.CAR);

        Vehicle newVehicle1 = new Vehicle(identificationNumber1, "Audi Q8", vehicleType1, vehicleLocation1, vehiclePrice1, restrictions1, 2L);

        vehicleRepository.save(newVehicle1);

        IdentificationNumber identificationNumber2 = new IdentificationNumber("DECBA321");
        VehicleType vehicleType2 = VehicleType.BICYCLE;
        Location vehicleLocation2 = new Location(51.482615100000004,7.409649777443613);
        Price vehiclePrice2 = new Price(0.5, BillingModel.PER_HOUR);
        VehicleRestrictions restrictions2 = new VehicleRestrictions(null, 60, 50.0, 1, DriverLicenseType.NONE);

        Vehicle newVehicle2 = new Vehicle(identificationNumber2, "Ultimate CF 7", vehicleType2, vehicleLocation2, vehiclePrice2, restrictions2, 2L);

        vehicleRepository.save(newVehicle2);
    }
}

