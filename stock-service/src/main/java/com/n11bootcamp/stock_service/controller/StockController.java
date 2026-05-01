package com.n11bootcamp.stock_service.controller;

import com.n11bootcamp.stock_service.dto.StockUpdateRequest;
import com.n11bootcamp.stock_service.dto.StockUpdateResponse;
import com.n11bootcamp.stock_service.entity.ProductStock;
import com.n11bootcamp.stock_service.repository.ProductStockRepository;
import com.n11bootcamp.stock_service.service.StockDomainService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks") // ✅ plural
public class StockController {
    private final StockDomainService stock;
    private final ProductStockRepository repo;

    public StockController(StockDomainService stock, ProductStockRepository repo) {
        this.stock = stock;
        this.repo = repo;
    }

    // ✅ GET — Tüm stokları listele
    @GetMapping
    public ResponseEntity<List<ProductStock>> getAllStocks() {
        return ResponseEntity.ok(repo.findAll());
    }

    // ✅ GET — Tekil ürün stok durumu
    @GetMapping("/{productId}")
    public ResponseEntity<ProductStock> getStockByProductId(@PathVariable Long productId) {
        return repo.findById(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/decrease")
    public ResponseEntity<StockUpdateResponse> decrease(@Valid @RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.decrease(req));
    }

    @PostMapping("/increase")
    public ResponseEntity<StockUpdateResponse> increase(@Valid @RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.increase(req));
    }

    @PostMapping("/reserve")
    public ResponseEntity<StockUpdateResponse> reserve(@Valid @RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.reserve(req));
    }

    @PostMapping("/release")
    public ResponseEntity<StockUpdateResponse> release(@Valid @RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.release(req));
    }

    @PostMapping("/commit")
    public ResponseEntity<StockUpdateResponse> commit(@Valid @RequestBody StockUpdateRequest req) {
        return ResponseEntity.ok(stock.commit(req));
    }
}
