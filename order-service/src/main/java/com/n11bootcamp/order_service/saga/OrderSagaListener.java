package com.n11bootcamp.order_service.saga;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.n11bootcamp.order_service.dto.payment.PaymentRequest;
import com.n11bootcamp.order_service.dto.payment.PaymentResponse;
import com.n11bootcamp.order_service.dto.stock.StockUpdateRequest;
import com.n11bootcamp.order_service.dto.stock.StockUpdateResponse;
import com.n11bootcamp.order_service.entity.Order;
import com.n11bootcamp.order_service.entity.OrderDetails;
import com.n11bootcamp.order_service.entity.OrderItem;
import com.n11bootcamp.order_service.entity.OrderStatus;
import com.n11bootcamp.order_service.repository.OrderRepository;
import com.n11bootcamp.order_service.service.PaymentServiceClient;
import com.n11bootcamp.order_service.service.StockServiceClient;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderSagaListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSagaListener.class);

    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final StockServiceClient stockServiceClient;
    private final PaymentCardStore paymentCardStore;

    public OrderSagaListener(OrderRepository orderRepository,
                             PaymentServiceClient paymentServiceClient,
                             StockServiceClient stockServiceClient,
                             PaymentCardStore paymentCardStore) {
        this.orderRepository = orderRepository;
        this.paymentServiceClient = paymentServiceClient;
        this.stockServiceClient = stockServiceClient;
        this.paymentCardStore = paymentCardStore;
    }

    @Transactional
    @RabbitListener(queues = "${order.rabbit.stockReservedQueue}")
    public void onStockReserved(StockReservedEvent event) {
        LOGGER.info("[SAGA] StockReservedEvent received: orderId={}, username={}, message={}",
                event.getOrderId(), event.getUsername(), event.getMessage());

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

        if (order.getStatus() != OrderStatus.CREATED) {
            LOGGER.warn("[SAGA] Ignoring stock reserved event for non-created order. status={}, orderId={}",
                    order.getStatus(), order.getId());
            return;
        }

        order.setStatus(OrderStatus.STOCK_DEDUCTED);
        orderRepository.save(order);

        PaymentRequest paymentRequest = toPaymentRequest(order);
        PaymentRequest.Card storedCard = paymentCardStore.take(order.getId());
        if (storedCard == null) {
            LOGGER.warn("[SAGA] Payment card is missing from temporary store. orderId={}", order.getId());
            markOrderCancelledAndReleaseStock(order);
            return;
        }
        paymentRequest.setCard(storedCard);

        PaymentResponse paymentResponse;
        try {
            paymentResponse = paymentServiceClient.makePayment(paymentRequest);
        } catch (Exception ex) {
            LOGGER.error("[SAGA] Payment service call failed. orderId={}", order.getId(), ex);
            markOrderCancelledAndReleaseStock(order);
            return;
        }

        if (paymentResponse == null || !paymentResponse.isSuccess()) {
            LOGGER.warn("[SAGA] Payment rejected. orderId={}, message={}",
                    order.getId(), paymentResponse != null ? paymentResponse.getMessage() : "null response");
            markOrderCancelledAndReleaseStock(order);
            return;
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        StockUpdateResponse commitResponse = commitReservedStock(order);
        if (commitResponse == null || !commitResponse.isSuccess()) {
            LOGGER.error("[SAGA] Stock commit failed. orderId={}, message={}",
                    order.getId(), commitResponse != null ? commitResponse.getMessage() : "null response");
            markOrderCancelledAndReleaseStock(order);
            return;
        }

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
        LOGGER.info("[SAGA] Order completed. orderId={}", order.getId());
    }

    @Transactional
    @RabbitListener(queues = "${order.rabbit.stockRejectedQueue}")
    public void onStockRejected(StockRejectedEvent event) {
        LOGGER.info("[SAGA] StockRejectedEvent received: orderId={}, username={}, message={}",
                event.getOrderId(), event.getUsername(), event.getMessage());

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            LOGGER.warn("[SAGA] Ignoring stock rejected event for final order. status={}, orderId={}",
                    order.getStatus(), order.getId());
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        LOGGER.info("[SAGA] Order cancelled because stock was rejected. orderId={}", order.getId());
    }

    private PaymentRequest toPaymentRequest(Order order) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(order.getId());
        request.setUsername(order.getUsername());
        request.setAmount(order.getTotalPrice());
        request.setPaymentMethod("MOCK");

        OrderDetails details = order.getOrderDetails();
        if (details != null) {
            request.setFirstName(details.getFirstName());
            request.setLastName(details.getLastName());
            request.setStreetAddress(details.getStreetAddress());
            request.setAddress(details.getStreetAddress());
            request.setCity(details.getCity());
            request.setCountry(details.getCountry());
            request.setPhone(details.getPhone());
            request.setEmail(details.getEmail());
        }

        List<PaymentRequest.Item> paymentItems = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            PaymentRequest.Item paymentItem = new PaymentRequest.Item();
            paymentItem.setProductId(item.getProductId());
            paymentItem.setProductName(item.getProductName());
            paymentItem.setPrice(item.getPrice());
            paymentItem.setQuantity(item.getQuantity());
            paymentItems.add(paymentItem);
        }
        request.setItems(paymentItems);

        return request;
    }

    private void markOrderCancelledAndReleaseStock(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        try {
            LOGGER.info("[SAGA] Releasing reserved stock. orderId={}", order.getId());
            StockUpdateResponse response = stockServiceClient.releaseStock(toStockUpdateRequest(order));
            if (response == null || !response.isSuccess()) {
                LOGGER.error("[SAGA] Stock release failed. orderId={}, message={}",
                        order.getId(), response != null ? response.getMessage() : "null response");
            }
        } catch (Exception ex) {
            LOGGER.error("[SAGA] Stock release call failed. orderId={}", order.getId(), ex);
        }
    }

    private StockUpdateResponse commitReservedStock(Order order) {
        try {
            LOGGER.info("[SAGA] Committing reserved stock. orderId={}", order.getId());
            return stockServiceClient.commitStock(toStockUpdateRequest(order));
        } catch (Exception ex) {
            LOGGER.error("[SAGA] Stock commit call failed. orderId={}", order.getId(), ex);
            return null;
        }
    }

    private StockUpdateRequest toStockUpdateRequest(Order order) {
        StockUpdateRequest request = new StockUpdateRequest();
        request.setOrderId(order.getId());

        List<StockUpdateRequest.StockItem> items = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            StockUpdateRequest.StockItem stockItem = new StockUpdateRequest.StockItem();
            stockItem.setProductId(item.getProductId());
            stockItem.setQuantity(item.getQuantity());
            items.add(stockItem);
        }
        request.setItems(items);
        return request;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StockReservedEvent {
        private Long orderId;
        private String username;
        private String message;

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StockRejectedEvent {
        private Long orderId;
        private String username;
        private String message;

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
