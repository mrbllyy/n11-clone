package com.n11bootcamp.order_service.service;


import com.n11bootcamp.order_service.dto.stock.StockUpdateRequest;
import com.n11bootcamp.order_service.dto.stock.StockUpdateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "stock-service", path = "/api/stocks")
public interface StockServiceClient {

    // 🟢 Stok düşürme (sipariş sırasında)
    @PostMapping("/decrease")
    @CircuitBreaker(name = "stockService")
    @Retry(name = "stockService")
    StockUpdateResponse decreaseStock(@RequestBody StockUpdateRequest request);

    // 🔵 Stok iadesi (ödeme başarısız vs.)
    @PostMapping("/increase")
    @CircuitBreaker(name = "stockService")
    @Retry(name = "stockService")
    StockUpdateResponse increaseStock(@RequestBody StockUpdateRequest request);
}
