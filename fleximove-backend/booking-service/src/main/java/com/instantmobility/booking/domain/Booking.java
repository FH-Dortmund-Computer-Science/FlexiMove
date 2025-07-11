package com.instantmobility.booking.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "BOOKING")
public class Booking {
    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private BookingId id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Column(name = "payment_id", columnDefinition = "BINARY(16)")
    private UUID paymentId;

    @Column(name = "booking_time")
    private LocalDateTime bookedAt;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "pickup_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "pickup_longitude"))
    })
    private GeoLocation pickupLocation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "dropoff_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "dropoff_longitude"))
    })
    private GeoLocation dropoffLocation;

    private double cost;

    @Embedded
    @AttributeOverrides({
            // Map all Trip fields to prefixed column names
            @AttributeOverride(name = "id", column = @Column(name = "trip_id")),
            @AttributeOverride(name = "startTime", column = @Column(name = "trip_start_time")),
            @AttributeOverride(name = "endTime", column = @Column(name = "trip_end_time")),
            @AttributeOverride(name = "startLatitude", column = @Column(name = "trip_start_latitude")),
            @AttributeOverride(name = "startLongitude", column = @Column(name = "trip_start_longitude")),
            @AttributeOverride(name = "endLatitude", column = @Column(name = "trip_end_latitude")),
            @AttributeOverride(name = "endLongitude", column = @Column(name = "trip_end_longitude")),
            @AttributeOverride(name = "completed", column = @Column(name = "trip_completed")),

            @AttributeOverride(name = "distance", column = @Column(name = "trip_distance")),
            @AttributeOverride(name = "trip_status", column = @Column(name = "trip_status")),

            // TimeFrame overrides
            @AttributeOverride(name = "timeFrame.startTime", column = @Column(name = "trip_start_time")),
            @AttributeOverride(name = "timeFrame.endTime", column = @Column(name = "trip_end_time")),

            // startLocation overrides
            @AttributeOverride(name = "startLocation.latitude", column = @Column(name = "trip_start_latitude")),
            @AttributeOverride(name = "startLocation.longitude", column = @Column(name = "trip_start_longitude")),

            // endLocation overrides
            @AttributeOverride(name = "endLocation.latitude", column = @Column(name = "trip_end_latitude")),
            @AttributeOverride(name = "endLocation.longitude", column = @Column(name = "trip_end_longitude"))
    })
    private Trip trip;

    public Booking(BookingId id, Long userId, Long vehicleId, LocalDateTime bookedAt, GeoLocation pickupLocation) {
        this.id = id;
        this.userId = userId;
        this.vehicleId = vehicleId;
        this.status = BookingStatus.CREATED;
        this.bookedAt = bookedAt;
        this.pickupLocation = pickupLocation;
        this.cost = 0.0;
        this.trip = new Trip(UUID.randomUUID());
    }

    public void confirm() {
        if (this.status != BookingStatus.CREATED) {
            throw new IllegalStateException("Booking cannot be confirmed from status: " + this.status);
        }
        this.status = BookingStatus.CONFIRMED;
    }

    public void startTrip(GeoLocation startLocation, LocalDateTime startTime) {
        if (this.status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Trip cannot be started from status: " + this.status);
        }
        TimeFrame tripTimeFrame = new TimeFrame(startTime, null);
        this.trip.recordLocation(startLocation);
        this.trip.setTimeFrame(tripTimeFrame);

        this.status = BookingStatus.STARTED;
    }

    public void endTrip(GeoLocation endLocation, LocalDateTime endTime) {
        if (this.status != BookingStatus.STARTED || this.trip == null) {
            throw new IllegalStateException("Trip cannot be ended from status: " + this.status);
        }

        this.trip.recordLocation(endLocation);
        this.trip.complete();

        if (this.trip.getTimeFrame().getEndTime() == null) {
            this.trip.getTimeFrame().setEndTime(endTime);
        }

        this.status = BookingStatus.COMPLETED;
        this.dropoffLocation = endLocation;
    }

    public void cancel() {
        if (this.status == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Completed booking cannot be cancelled");
        }

        if (this.status == BookingStatus.STARTED) {
            throw new IllegalStateException("Booking cannot be cancelled. Trip hast to be finished and payed first.");
        }

        this.status = BookingStatus.CANCELLED;
    }

    public void calculateCost(String billingModel, double rate) {
        if (this.status != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot calculate cost: trip not completed");
        }

        if (this.trip == null) {
            throw new IllegalStateException("Cannot calculate cost: no trip information available");
        }

        double distanceInKm = this.trip.getDistanceInKm();
        this.trip.setDistance(distanceInKm);
        long durationInMinutes = this.trip.getDurationInMinutes();

        double calculatedCost;
        if ("PER_HOUR".equals(billingModel)) {
            double hours = durationInMinutes / 60.0;
            calculatedCost = rate * hours;
        } else {
            calculatedCost = rate * distanceInKm;
        }

        // Round to 2 decimal places
        calculatedCost = Math.round(calculatedCost * 100.0) / 100.0;

        // Set the cost
        this.cost = calculatedCost;
    }

    public TripSummary createTripSummary(String billingModel, double rate) {
        if (this.status != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot create summary: trip not completed");
        }

        TripSummary summary = new TripSummary();
        summary.setBookingId(this.id.getValue());
        summary.setCost(this.cost);
        summary.setDistanceInKm(this.trip.getDistanceInKm());
        summary.setDurationInMinutes(this.trip.getDurationInMinutes());
        summary.setBillingModel(billingModel);
        summary.setRate(rate);
        summary.setStartTime(this.trip.getTimeFrame().getStartTime());
        summary.setEndTime(this.trip.getTimeFrame().getEndTime());
        summary.setPickupLocation(this.pickupLocation);
        summary.setDropoffLocation(this.dropoffLocation);

        return summary;
    }

    public GeoLocation getFinalLocation() {
        if (this.status == BookingStatus.COMPLETED && this.dropoffLocation != null) {
            return this.dropoffLocation;
        } else if (this.trip != null && !this.trip.getRoute().isEmpty()) {
            // Return last recorded location in trip
            List<GeoLocation> route = this.trip.getRoute();
            return route.get(route.size() - 1);
        }
        return null;
    }
}

