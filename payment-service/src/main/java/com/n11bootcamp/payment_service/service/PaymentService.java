package com.n11bootcamp.payment_service.service;

import com.n11bootcamp.payment_service.dto.PaymentRequest;
import com.n11bootcamp.payment_service.dto.PaymentResponse;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    @Value("${payment.mock.force-failure:false}")
    private boolean forceFailure;

    public PaymentResponse pay(PaymentRequest request) {
        if (forceFailure) {
            return PaymentResponse.fail("Payment provider is configured to fail");
        }

        if (request.getAmount() == null || request.getAmount() <= 0) {
            return PaymentResponse.fail("Amount must be greater than zero");
        }

        PaymentRequest.Card card = request.getCard();
        if (card == null || isBlank(card.getCardNumber()) || isBlank(card.getCvc())) {
            return PaymentResponse.fail("Card information is required");
        }

        String normalizedCardNumber = card.getCardNumber().replaceAll("\\s+", "");
        if (normalizedCardNumber.endsWith("0000") || "000".equals(card.getCvc())) {
            return PaymentResponse.fail("Payment declined by mock provider");
        }

        return PaymentResponse.ok("mock-" + UUID.randomUUID());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
