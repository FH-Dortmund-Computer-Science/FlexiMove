package com.payment.service.service;

import com.payment.service.domain.entity.Payment;
import com.payment.service.domain.enums.PaymentStatus;
import com.payment.service.domain.repo.PaymentRepository;
import com.payment.service.infrastructure.PaymentGatewayClient;
import com.payment.service.service.DTO.PaymentRequestDTO;
import com.payment.service.service.DTO.PaymentResponseDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentProcessingService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayClient gatewayClient;
    private final PaymentEventPublisher eventPublisher;

    public PaymentProcessingService(PaymentRepository paymentRepository,
                                    PaymentGatewayClient gatewayClient,
                                    PaymentEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.gatewayClient = gatewayClient;
        this.eventPublisher = eventPublisher;
    }

    public PaymentResponseDTO processPayment(PaymentRequestDTO dto) {
        // Build Payment entity from DTO
        Payment payment = new Payment();
        payment.setBookingId(dto.getBookingId());
        payment.setUserId(dto.getUserId());
        payment.setAmount(dto.getAmount());
        payment.setCurrency(dto.getCurrency());
        payment.setDescription(dto.getDescription());
        payment.setTimestamp(LocalDateTime.now());
        payment.setPaymentStatus(PaymentStatus.PENDING); // Initial state

        // Charge using gateway
        try {
            boolean success = gatewayClient.charge(payment);
            if (success) {
                payment.markAsCompleted();
            } else {
                payment.markAsFailed();
            }
        } catch (Exception e) {
            payment.markAsFailed();
        }

        // Save and publish event
        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishPaymentProcessed(saved);

        return new PaymentResponseDTO(
                saved.getPaymentId(),
                saved.getPaymentStatus().toString(),
                saved.getPaymentStatus() == PaymentStatus.COMPLETED ?
                        "Payment successful" : "Payment failed",
                saved.getBookingId(),
                saved.getAmount(),
                saved.getCurrency(),
                saved.getDescription()
        );
    }
}
