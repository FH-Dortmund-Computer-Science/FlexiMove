package com.instantmobility.booking.service;

import com.instantmobility.booking.clients.PaymentServiceClient;
import com.instantmobility.booking.clients.VehicleServiceClient;
import com.instantmobility.booking.domain.*;
import com.instantmobility.booking.dto.*;
import com.instantmobility.booking.repository.BookingRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final PaymentServiceClient paymentServiceClient;

    private final VehicleServiceClient vehicleServiceClient;

    @Autowired
    public BookingService(BookingRepository bookingRepository, PaymentServiceClient paymentServiceClient, VehicleServiceClient vehicleServiceClient) {
        this.bookingRepository = bookingRepository;
        this.paymentServiceClient = paymentServiceClient;
        this.vehicleServiceClient = vehicleServiceClient;
    }


    public UUID createBooking(CreateBookingRequest request) {

        validateUserForVehicle(request);
        // Create booking domain object
        BookingId bookingId = BookingId.generate();
        TimeFrame timeFrame = new TimeFrame(request.getStartTime());
        GeoLocation pickupLocation = new GeoLocation(request.getPickupLatitude(), request.getPickupLongitude());

        Booking booking = new Booking(bookingId, request.getUserId(), request.getVehicleId(), timeFrame, pickupLocation);
        //TODO in real application: check whether user fulfills the vehicle restrictions; request to the vehicleServiceClient
        //TODO for now: only check whether the user has neccessary license and his age
        booking.confirm();

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            vehicleServiceClient.updateVehicleStatus(booking.getVehicleId(), VehicleStatus.BOOKED);
        }
        bookingRepository.save(booking);

        return bookingId.getValue();
    }

    private void validateUserForVehicle(CreateBookingRequest request) {
        // Age validation
        if (request.getUserAge() < request.getVehicleMinimumAge()) {
            throw new ValidationException(
                    "User age (" + request.getUserAge() + ") is below the minimum required age ("
                            + request.getVehicleMinimumAge() + ") for this vehicle"
            );
        }

        // License validation
        if (request.getVehicleRequiredLicense() != null && !request.getVehicleRequiredLicense().isEmpty()) {
            if (request.getUserLicenseType() == null || request.getUserLicenseType().isEmpty()) {
                throw new ValidationException("User license is required for this vehicle");
            }

            // Case-insensitive string comparison
            if (!request.getUserLicenseType().equalsIgnoreCase(request.getVehicleRequiredLicense())) {
                throw new ValidationException(
                        "User license type (" + request.getUserLicenseType()
                                + ") does not match the required license for this vehicle: "
                                + request.getVehicleRequiredLicense()
                );
            }
        }
    }

    public void startTrip(UUID bookingId, StartTripRequest request) {
        Booking booking = getBookingById(bookingId);

        GeoLocation startLocation = new GeoLocation(request.getStartLatitude(), request.getStartLongitude());
        LocalDateTime startTime = request.getStartTime() != null ? request.getStartTime() : LocalDateTime.now();

        booking.startTrip(startLocation, startTime);
        if (booking.getStatus() == BookingStatus.STARTED) {
            vehicleServiceClient.updateVehicleStatus(booking.getVehicleId(), VehicleStatus.IN_USE);
        }
        bookingRepository.save(booking);
    }

//    public void endTrip(UUID bookingId, EndTripRequest request) {
//        Booking booking = getBookingById(bookingId);
//
//        GeoLocation endLocation = new GeoLocation(request.getEndLatitude(), request.getEndLongitude());
//        LocalDateTime endTime = request.getEndTime() != null ? request.getEndTime() : LocalDateTime.now();
//
//        booking.endTrip(endLocation, endTime);
//
//        // Get billing info from vehicle service
//
//
//        //TODO: trigger Vehicle Service to get data about billing model and price
//        //TODO: totalCost = trigger Payment Service and send to it duration, distance, price and bbilling model
//        // Get billing info from vehicle service
//        BillingInfo billingInfo = vehicleService.getBillingInfo(booking.getVehicleId());
//
//
//
//
//        try {
//            //processPaymentForBooking(booking);
//        } catch (Exception e) {
//            throw new RuntimeException("Payment failed for booking " + bookingId + ": " + e.getMessage(), e);
//        }
//
//        if (booking.getStatus() == BookingStatus.COMPLETED) {
//            try {
//                updateVehicleLocationAndStatus(booking.getVehicleId(), endLocation);
//            } catch (Exception e) {
//                throw new RuntimeException("Failed to update vehicle location or status for booking " + bookingId + ": " + e.getMessage(), e);
//            }
//        }
//
//        bookingRepository.save(booking);
//    }

    @Transactional
    public TripSummary endTrip(UUID bookingId, EndTripRequest request) {
        Booking booking = getBookingById(bookingId);

        // End the trip
        GeoLocation endLocation = new GeoLocation(request.getEndLatitude(), request.getEndLongitude());
        LocalDateTime endTime = request.getEndTime() != null ? request.getEndTime() : LocalDateTime.now();
        booking.endTrip(endLocation, endTime);

        // Get billing info from vehicle service
        BillingInfo billingInfo = vehicleServiceClient.getBillingInfo(booking.getVehicleId());

        // Calculate cost based on billing model
        booking.calculateCost(billingInfo.getBillingModel(), billingInfo.getRate());

        // Create trip summary
        TripSummary summary = booking.createTripSummary(
                billingInfo.getBillingModel(),
                billingInfo.getRate()
        );

        // Save booking with updated cost
        bookingRepository.save(booking);

        return summary;
    }

    @Transactional
    public PaymentResponse processPayment(UUID bookingId, PaymentRequest request) {
        // Validate booking exists and belongs to the user
        Booking booking = getBookingById(bookingId);

        if (!booking.getUserId().equals(request.getUserId())) {
            throw new ValidationException("Booking does not belong to this user");
        }

        // Validate booking is in the right state
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Booking must be in COMPLETED state to process payment, current state: " + booking.getStatus()
            );
        }

        // Create payment request for payment service
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setBookingId(bookingId);
        paymentRequest.setUserId(request.getUserId());
        paymentRequest.setAmount(BigDecimal.valueOf(booking.getCost()));
        paymentRequest.setPaymentMethod(request.getPaymentMethod());
        paymentRequest.setDescription("Payment for booking " + bookingId);

        // Call payment service
        PaymentResponse response = paymentServiceClient.processPayment(paymentRequest);

        // Update booking status based on payment result
        if ("SUCCESS".equals(response.getStatus())) {
            booking.setStatus(BookingStatus.PAID);
            booking.setPaymentId(response.getPaymentId());
            bookingRepository.save(booking);
        }

        return response;
    }

    public void cancelBooking(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        booking.cancel();
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            vehicleServiceClient.updateVehicleStatus(booking.getVehicleId(), VehicleStatus.AVAILABLE);
        }
        bookingRepository.save(booking);
    }

    /**
     * Updates vehicle location when booking ends
     */
    private void updateVehicleLocationAndStatus(Long vehicleId, GeoLocation location) {
        vehicleServiceClient.updateVehicleLocation(vehicleId, location);
        vehicleServiceClient.updateVehicleStatus(vehicleId, VehicleStatus.AVAILABLE);
        System.out.println("Vehicle " + vehicleId + " location updated to: " +
                location.getLatitude() + ", " + location.getLongitude());
    }

    private void processPaymentForBooking(Booking booking) {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setUserId(booking.getUserId());
        paymentRequest.setBookingId(booking.getId().getValue());
        paymentRequest.setAmount(BigDecimal.valueOf(booking.getCost()));
        paymentRequest.setDescription("Ride payment for booking " + booking.getId().getValue());

        try {
            PaymentResponse response = paymentServiceClient.processPayment(paymentRequest);
            // Store payment reference in booking or handle accordingly
            System.out.println("Payment processed: " + response.getPaymentId());
        } catch (Exception e) {
            // Handle payment failure
            System.err.println("Payment failed: " + e.getMessage());
        }
    }

    /**
     * Deletes all bookings for a specific user
     */
    @Transactional
    public void deleteBookingsByUserId(Long userId) {
        List<Booking> userBookings = bookingRepository.findByUserId(userId);

        if (!userBookings.isEmpty()) {
           for (Booking booking : userBookings) {
                // Don't allow deletion of active bookings
                if (booking.getStatus() != BookingStatus.PAID &&
                        booking.getStatus() != BookingStatus.CANCELLED) {
                    throw new IllegalStateException(
                            "Cannot delete user with active or not paid bookings. Either bookings have to be paid or trips have to be cancelled first.");
                }
            }

            try {
                boolean hasPaidBookings = userBookings.stream()
                        .anyMatch(b -> b.getStatus() == BookingStatus.PAID);

                if (hasPaidBookings) {
                    try {
                        paymentServiceClient.deletePaymentsByUser(userId);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to delete payment info for user " + userId, e);
                    }
                }
                bookingRepository.deleteByUserId(userId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete bookings for user " + userId, e);
            }
        }
    }

    @Transactional
    public void deleteBookingsAndPaymentsByVehicle(Long vehicleId) {
        List<Booking> bookings = bookingRepository.findByVehicleId(vehicleId);

        if (!bookings.isEmpty()) {
            for (Booking booking : bookings) {
                // Don't allow deletion of active bookings
                if (booking.getStatus() != BookingStatus.PAID &&
                        booking.getStatus() != BookingStatus.CANCELLED) {
                    throw new IllegalStateException(
                            "Cannot delete vehicle with active or not paid booking.");
                }
            }

            try {
                List<Booking> deletableBookings = bookings.stream()
                        .filter(b -> b.getStatus() == BookingStatus.PAID)
                        .toList();

                if (!deletableBookings.isEmpty()) {
                    try {
                        for (Booking b : deletableBookings) {
                            paymentServiceClient.deletePaymentsByBookingId(b.getId().getValue());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to delete payment info for vehicle " + vehicleId, e);
                    }
                }
                bookingRepository.deleteByVehicleId(vehicleId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete bookings for vehicle " + vehicleId, e);
            }
        }
    }


    /**
     * Gets booking history for a user with pagination
     
    public List<BookingDTO> getBookingHistory(Long userId, int page, int size) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByTimeFrameDesc(userId, page, size);
        return bookings.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
*/
    public BookingDTO getBookingDetails(UUID bookingId) {
        Booking booking = getBookingById(bookingId);
        return mapToDTO(booking);
    }

    public List<BookingDTO> getUserBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookings.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private Booking getBookingById(UUID bookingId) {
        return bookingRepository.findById(new BookingId(bookingId))
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));
    }

    private BookingDTO mapToDTO(Booking booking) {
        BookingDTO dto = new BookingDTO();
        dto.setId(booking.getId().getValue());
        dto.setUserId(booking.getUserId());
        dto.setVehicleId(booking.getVehicleId());
        dto.setStatus(booking.getStatus().name());
        dto.setStartTime(booking.getTimeFrame().getStartTime());
        dto.setEndTime(booking.getTimeFrame().getEndTime());
        dto.setCost(booking.getCost());

        if (booking.getPickupLocation() != null) {
            dto.setPickupLatitude(booking.getPickupLocation().getLatitude());
            dto.setPickupLongitude(booking.getPickupLocation().getLongitude());
        }

        if (booking.getDropoffLocation() != null) {
            dto.setDropoffLatitude(booking.getDropoffLocation().getLatitude());
            dto.setDropoffLongitude(booking.getDropoffLocation().getLongitude());
        }

        if (booking.getTrip() != null) {
            dto.setDistance(booking.getTrip().getDistance());
        }

        return dto;
    }
}
