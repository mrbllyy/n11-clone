package com.n11bootcamp.order_service.service;

import com.n11bootcamp.order_service.dto.payment.PaymentRequest;
import com.n11bootcamp.order_service.dto.payment.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "payment-service") // Eureka üzerinden discovery
public interface PaymentServiceClient {

    @PostMapping("/api/payments/pay")
    @CircuitBreaker(name = "paymentService")
    @Retry(name = "paymentService")
    PaymentResponse makePayment(@RequestBody PaymentRequest request);

}
