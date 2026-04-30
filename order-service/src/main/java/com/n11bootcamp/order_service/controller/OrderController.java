package com.n11bootcamp.order_service.controller;

import com.n11bootcamp.order_service.dto.CreateOrderRequest;
import com.n11bootcamp.order_service.dto.OrderResponse;
import com.n11bootcamp.order_service.service.OrderService;
import com.n11bootcamp.order_service.service.impl.OrderServiceImpl;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderServiceImpl orderServiceImpl;
    public OrderController(OrderServiceImpl orderServiceImpl) {
        this.orderServiceImpl = orderServiceImpl;
    }

    @PostMapping
    public OrderResponse createOrder(@RequestHeader(value = "X-User-Name", required = false) String username,
                                     @RequestBody CreateOrderRequest request) {
        // Gateway'den gelen güvenilir kullanıcı adını ezerek güvenlik sağlar
        if (username != null) {
            request.setUsername(username);
        }
        return orderServiceImpl.createOrder(request);
    }

    // api/orders çağrıldığında kullanıcının kendi siparişlerini döner
    @GetMapping
    public List<OrderResponse> getMyOrders(@RequestHeader(value = "X-User-Name", required = false) String username) {
        if (username == null) {
            throw new RuntimeException("Yetkisiz erişim: Kullanıcı bilgisi bulunamadı");
        }
        return orderServiceImpl.findOrdersByUsername(username);
    }

    @GetMapping("/all")
    public List<OrderResponse> getAllOrders() {
        return orderServiceImpl.findAllOrders();
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable Long id) {
        return orderServiceImpl.getOrderById(id);
    }

    @GetMapping("/user/{username}")
    public List<OrderResponse> getOrdersByUser(@PathVariable String username) {
        return orderServiceImpl.findOrdersByUsername(username);
    }
}
