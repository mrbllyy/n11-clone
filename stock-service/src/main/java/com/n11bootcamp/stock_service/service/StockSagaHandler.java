package com.n11bootcamp.stock_service.service;


import com.n11bootcamp.stock_service.dto.EventPayloads;
import com.n11bootcamp.stock_service.dto.StockUpdateRequest;
import com.n11bootcamp.stock_service.dto.StockUpdateResponse;
import com.n11bootcamp.stock_service.entity.StockReservation;
import com.n11bootcamp.stock_service.entity.StockReservationStatus;
import com.n11bootcamp.stock_service.repository.StockReservationRepository;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class StockSagaHandler {

    private final StockDomainService stock;
    private final RabbitTemplate rabbit;
    private final StockReservationRepository reservationRepository;

    @Value("${stock.rabbit.exchange}")
    private String exchange;

    @Value("${stock.rabbit.reservedRoutingKey}")
    private String reservedRoutingKey;

    @Value("${stock.rabbit.rejectedRoutingKey}")
    private String rejectedRoutingKey;

    public StockSagaHandler(StockDomainService stock,
                            RabbitTemplate rabbit,
                            StockReservationRepository reservationRepository) {
        this.stock = stock;
        this.rabbit = rabbit;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    @RabbitListener(queues = "${stock.rabbit.reserveRequestedQueue}")
    public void handleReserveRequested(EventPayloads.StockReserveRequestedEvent event) {
        if (event.getOrderId() == null) {
            publishRejected(event, "Order id is required");
            return;
        }
        if (event.getItems() == null || event.getItems().isEmpty()) {
            publishRejected(event, "Stock items are required");
            return;
        }

        StockReservation existing = reservationRepository.findById(event.getOrderId()).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == StockReservationStatus.RESERVED
                    || existing.getStatus() == StockReservationStatus.COMMITTED) {
                publishReserved(event, "Stock reservation already accepted");
                return;
            }
            publishRejected(event, existing.getMessage() != null ? existing.getMessage() : "Stock reservation already rejected");
            return;
        }

        StockUpdateRequest req = new StockUpdateRequest(
                event.getItems().stream()
                        .map(i -> new StockUpdateRequest.StockItem(i.getProductId(), i.getQuantity()))
                        .collect(Collectors.toList())
        );
        req.setOrderId(event.getOrderId());

        StockUpdateResponse resp = stock.reserve(req);

        if (resp.isSuccess()) {
            publishReserved(event, "Stock reserved");
        } else {
            publishRejected(event, resp.getMessage());
        }
    }

    private void publishReserved(EventPayloads.StockReserveRequestedEvent event, String message) {
        EventPayloads.StockReservedEvent reserved =
                new EventPayloads.StockReservedEvent(event.getOrderId(), event.getUsername(), message);
        rabbit.convertAndSend(exchange, reservedRoutingKey, reserved);
    }

    private void publishRejected(EventPayloads.StockReserveRequestedEvent event, String message) {
        EventPayloads.StockRejectedEvent rejected =
                new EventPayloads.StockRejectedEvent(event.getOrderId(), event.getUsername(), message);
        rabbit.convertAndSend(exchange, rejectedRoutingKey, rejected);
    }
}
