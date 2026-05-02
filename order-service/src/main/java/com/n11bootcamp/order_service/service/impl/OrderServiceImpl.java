package com.n11bootcamp.order_service.service.impl;
import com.n11bootcamp.order_service.dto.CreateOrderRequest;
import com.n11bootcamp.order_service.dto.OrderResponse;
import com.n11bootcamp.order_service.dto.payment.PaymentRequest;
import com.n11bootcamp.order_service.dto.stock.StockReserveRequestedEvent;
import com.n11bootcamp.order_service.entity.Order;
import com.n11bootcamp.order_service.entity.OrderDetails;
import com.n11bootcamp.order_service.entity.OrderItem;
import com.n11bootcamp.order_service.entity.OrderStatus;
import com.n11bootcamp.order_service.event.OrderCreatedEvent;
import com.n11bootcamp.order_service.repository.OrderRepository;
import com.n11bootcamp.order_service.saga.PaymentCardStore;
import com.n11bootcamp.order_service.service.OrderService;
import com.n11bootcamp.order_service.service.PaymentServiceClient;
import com.n11bootcamp.order_service.service.ProductServiceClient;
import com.n11bootcamp.order_service.service.StockServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final StockServiceClient stockServiceClient;
    private final ProductServiceClient productServiceClient;
    private final ApplicationEventPublisher publisher;
    private final RabbitTemplate rabbitTemplate;
    private final PaymentCardStore paymentCardStore;

    @Value("${stock.rabbit.exchange}")
    private String stockExchange;

    @Value("${stock.rabbit.reserveRequestedRoutingKey}")
    private String stockReserveRequestedRoutingKey;

    public OrderServiceImpl(OrderRepository orderRepository,
                            PaymentServiceClient paymentServiceClient,
                            StockServiceClient stockServiceClient,
                            ProductServiceClient productServiceClient,
                            ApplicationEventPublisher publisher,
                            RabbitTemplate rabbitTemplate,
                            PaymentCardStore paymentCardStore) {
        this.orderRepository = orderRepository;
        this.paymentServiceClient = paymentServiceClient;
        this.stockServiceClient = stockServiceClient;
        this.productServiceClient = productServiceClient;
        this.publisher = publisher;
        this.rabbitTemplate = rabbitTemplate;
        this.paymentCardStore = paymentCardStore;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        // 1️⃣ Ürün bilgilerini tamamla (İsim ve Fiyat)
        for (CreateOrderRequest.OrderItemDto dto : request.getItems()) {
            try {
                Map<String, Object> product = productServiceClient.getProductById(dto.getProductId());
                if (product != null) {
                    dto.setProductName((String) product.get("title"));
                    Object priceObj = product.get("price");
                    if (priceObj instanceof Number) {
                        dto.setPrice(((Number) priceObj).doubleValue());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Product info could not be fetched for productId={}: {}", dto.getProductId(), e.getMessage());
                throw new RuntimeException("Ürün bilgileri alınamadı: " + dto.getProductId());
            }
        }

        // 2️⃣ Order entity oluştur → CREATED
        Order order = new Order();
        order.setUsername(request.getUsername());
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(
                request.getItems().stream()
                        .mapToDouble(i -> (i.getPrice() != null ? i.getPrice() : 0.0) * i.getQuantity())
                        .sum()
        );

        // 2.1️⃣ OrderItem mapping
        List<OrderItem> items = request.getItems().stream().map(dto -> {
            OrderItem item = new OrderItem();
            item.setProductId(dto.getProductId());
            item.setProductName(dto.getProductName());
            item.setPrice(dto.getPrice());
            item.setQuantity(dto.getQuantity());
            item.setOrder(order);
            return item;
        }).collect(Collectors.toList());
        order.setItems(items);

        // 2.2️⃣ OrderDetails mapping (AddressInfo'dan)
        OrderDetails details = new OrderDetails();
        if (request.getAddressInfo() != null) {
            String fullName = request.getAddressInfo().getFullName();
            if (fullName != null && fullName.contains(" ")) {
                int firstSpace = fullName.indexOf(" ");
                details.setFirstName(fullName.substring(0, firstSpace));
                details.setLastName(fullName.substring(firstSpace + 1));
            } else {
                details.setFirstName(fullName);
                details.setLastName("");
            }
            details.setStreetAddress(request.getAddressInfo().getFullAddress());
            details.setCity(request.getAddressInfo().getCity());
            details.setDistrict(request.getAddressInfo().getDistrict());
            details.setPhone(request.getAddressInfo().getPhone());
            details.setCountry("TR"); // Default veya request'ten alınabilir
            details.setEmail("user@example.com"); // Username'den veya request'ten alınabilir
        }
        details.setOrder(order);
        order.setOrderDetails(details);

        // 2.3️⃣ Order DB'ye kaydet
        Order savedOrder = orderRepository.save(order);
        LOGGER.info("Order CREATED kaydedildi. orderId={}, username={}, totalPrice={}",
                savedOrder.getId(), savedOrder.getUsername(), savedOrder.getTotalPrice());

        // 2.4️⃣ Kart bilgisini RAM store'a sakla
        if (request.getPaymentCard() != null) {
            PaymentRequest.Card cardForStore = new PaymentRequest.Card();
            cardForStore.setCardHolderName(request.getPaymentCard().getCardHolderName());
            cardForStore.setCardNumber(request.getPaymentCard().getCardNumber());
            cardForStore.setCvc(request.getPaymentCard().getCvv());
            
            // "MM/YY" formatını ayır
            String expireDate = request.getPaymentCard().getExpireDate();
            if (expireDate != null && expireDate.contains("/")) {
                String[] parts = expireDate.split("/");
                cardForStore.setExpireMonth(parts[0]);
                cardForStore.setExpireYear(parts[1].length() == 2 ? "20" + parts[1] : parts[1]);
            }
            
            paymentCardStore.put(savedOrder.getId(), cardForStore);
            LOGGER.info("Kart bilgisi RAM store'a kaydedildi. orderId={}", savedOrder.getId());
        }

        // 3️⃣ Saga: StockReserveRequestedEvent yayınla
        StockReserveRequestedEvent eventPayload = new StockReserveRequestedEvent();
        eventPayload.setOrderId(savedOrder.getId());
        eventPayload.setUsername(savedOrder.getUsername());
        eventPayload.setItems(savedOrder.getItems().stream()
                .map(it -> new StockReserveRequestedEvent.Item(it.getProductId(), it.getQuantity()))
                .collect(Collectors.toList()));

        publishStockReserveAfterCommit(eventPayload);

        // 4️⃣ Response dön
        OrderResponse response = new OrderResponse();
        response.setOrderId(savedOrder.getId());
        response.setUsername(savedOrder.getUsername());
        response.setStatus(savedOrder.getStatus().name());
        response.setTotalPrice(savedOrder.getTotalPrice());
        response.setItems(savedOrder.getItems().stream().map(item -> {
            OrderResponse.OrderItemResponse i = new OrderResponse.OrderItemResponse();
            i.setProductId(item.getProductId());
            i.setProductName(item.getProductName());
            i.setPrice(item.getPrice());
            i.setQuantity(item.getQuantity());
            return i;
        }).collect(Collectors.toList()));

        return response;
    }

    private void publishStockReserveAfterCommit(StockReserveRequestedEvent eventPayload) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishStockReserve(eventPayload);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishStockReserve(eventPayload);
            }
        });
    }

    private void publishStockReserve(StockReserveRequestedEvent eventPayload) {
        rabbitTemplate.convertAndSend(stockExchange, stockReserveRequestedRoutingKey, eventPayload);
    }

    @Override
    public List<OrderResponse> findAllOrders() {
        return orderRepository.findAll().stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    @Override
    public OrderResponse getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::mapToOrderResponse)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Override
    public List<OrderResponse> findOrdersByUsername(String username) {
        return orderRepository.findByUsername(username).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse res = new OrderResponse();
        res.setOrderId(order.getId());
        res.setUsername(order.getUsername());
        res.setStatus(order.getStatus().name());
        res.setTotalPrice(order.getTotalPrice());
        res.setItems(order.getItems().stream().map(item -> {
            OrderResponse.OrderItemResponse i = new OrderResponse.OrderItemResponse();
            i.setProductId(item.getProductId());
            i.setProductName(item.getProductName());
            i.setPrice(item.getPrice());
            i.setQuantity(item.getQuantity());
            return i;
        }).collect(Collectors.toList()));
        return res;
    }
}
