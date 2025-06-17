package com.instantmobility.booking.clients;

import com.instantmobility.booking.dto.PaymentRequestDTO;
import com.instantmobility.booking.dto.PaymentResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {
    @PostMapping("/api/payments/process")
    PaymentResponseDTO processPayment(@RequestBody PaymentRequestDTO request);

    @GetMapping("/api/payments/{paymentId}")
    PaymentResponseDTO getPaymentStatus(@PathVariable UUID paymentId);

}
