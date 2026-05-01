package com.n11bootcamp.stock_service.service;

import com.n11bootcamp.stock_service.dto.StockUpdateRequest;
import com.n11bootcamp.stock_service.dto.StockUpdateResponse;
import com.n11bootcamp.stock_service.entity.ProductStock;
import com.n11bootcamp.stock_service.entity.StockReservation;
import com.n11bootcamp.stock_service.entity.StockReservationStatus;
import com.n11bootcamp.stock_service.repository.ProductStockRepository;
import com.n11bootcamp.stock_service.repository.StockReservationRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class StockDomainService {

    private final ProductStockRepository repo;
    private final StockReservationRepository reservationRepository;

    public StockDomainService(ProductStockRepository repo,
                              StockReservationRepository reservationRepository) {
        this.repo = repo;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public StockUpdateResponse decrease(StockUpdateRequest req) {
        try {
            List<ProductStock> stocks = loadAndValidateAvailable(req);
            apply(req, stocks, StockOperation.DECREASE);
            return StockUpdateResponse.ok("Stock decreased");
        } catch (Exception e) {
            return StockUpdateResponse.fail(e.getMessage());
        }
    }

    @Transactional
    public StockUpdateResponse increase(StockUpdateRequest req) {
        try {
            List<ProductStock> stocks = loadAndValidateExisting(req);
            apply(req, stocks, StockOperation.INCREASE);
            return StockUpdateResponse.ok("Stock increased");
        } catch (Exception e) {
            return StockUpdateResponse.fail(e.getMessage());
        }
    }

    @Transactional
    public StockUpdateResponse reserve(StockUpdateRequest req) {
        try {
            List<ProductStock> stocks = loadAndValidateAvailable(req);
            apply(req, stocks, StockOperation.RESERVE);
            markReservation(req.getOrderId(), StockReservationStatus.RESERVED, "Stock reserved");
            return StockUpdateResponse.ok("Stock reserved");
        } catch (Exception e) {
            markReservation(req != null ? req.getOrderId() : null, StockReservationStatus.REJECTED, e.getMessage());
            return StockUpdateResponse.fail(e.getMessage());
        }
    }

    @Transactional
    public StockUpdateResponse release(StockUpdateRequest req) {
        try {
            Optional<StockReservation> existing = findReservation(req.getOrderId());
            if (existing.isPresent()) {
                StockReservationStatus status = existing.get().getStatus();
                if (status == StockReservationStatus.RELEASED) {
                    return StockUpdateResponse.ok("Stock reservation already released");
                }
                if (status == StockReservationStatus.REJECTED) {
                    return StockUpdateResponse.ok("Stock reservation was already rejected");
                }
                if (status == StockReservationStatus.COMMITTED) {
                    return StockUpdateResponse.fail("Stock reservation is already committed");
                }
            }

            List<ProductStock> stocks = loadAndValidateReserved(req);
            apply(req, stocks, StockOperation.RELEASE);
            markReservation(req.getOrderId(), StockReservationStatus.RELEASED, "Stock released");
            return StockUpdateResponse.ok("Stock released");
        } catch (Exception e) {
            return StockUpdateResponse.fail(e.getMessage());
        }
    }

    @Transactional
    public StockUpdateResponse commit(StockUpdateRequest req) {
        try {
            Optional<StockReservation> existing = findReservation(req.getOrderId());
            if (existing.isPresent()) {
                StockReservationStatus status = existing.get().getStatus();
                if (status == StockReservationStatus.COMMITTED) {
                    return StockUpdateResponse.ok("Stock reservation already committed");
                }
                if (status == StockReservationStatus.RELEASED || status == StockReservationStatus.REJECTED) {
                    return StockUpdateResponse.fail("Stock reservation cannot be committed from status " + status);
                }
            }

            List<ProductStock> stocks = loadAndValidateReserved(req);
            apply(req, stocks, StockOperation.COMMIT);
            markReservation(req.getOrderId(), StockReservationStatus.COMMITTED, "Stock committed");
            return StockUpdateResponse.ok("Stock committed");
        } catch (Exception e) {
            return StockUpdateResponse.fail(e.getMessage());
        }
    }

    private List<ProductStock> loadAndValidateAvailable(StockUpdateRequest req) {
        List<ProductStock> stocks = loadAndValidateExisting(req);
        for (int i = 0; i < req.getItems().size(); i++) {
            StockUpdateRequest.StockItem item = req.getItems().get(i);
            ProductStock stock = stocks.get(i);
            if (stock.getAvailableQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for productId=" + item.getProductId());
            }
        }
        return stocks;
    }

    private List<ProductStock> loadAndValidateReserved(StockUpdateRequest req) {
        List<ProductStock> stocks = loadAndValidateExisting(req);
        for (int i = 0; i < req.getItems().size(); i++) {
            StockUpdateRequest.StockItem item = req.getItems().get(i);
            ProductStock stock = stocks.get(i);
            if (stock.getReservedQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient reserved stock for productId=" + item.getProductId());
            }
        }
        return stocks;
    }

    private List<ProductStock> loadAndValidateExisting(StockUpdateRequest req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Stock items are required");
        }

        List<ProductStock> stocks = new ArrayList<>();
        for (StockUpdateRequest.StockItem item : req.getItems()) {
            if (item.getProductId() == null) {
                throw new IllegalArgumentException("Product id is required");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }
            ProductStock stock = repo.findByProductIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProductId()));
            stocks.add(stock);
        }
        return stocks;
    }

    private void apply(StockUpdateRequest req, List<ProductStock> stocks, StockOperation operation) {
        for (int i = 0; i < req.getItems().size(); i++) {
            StockUpdateRequest.StockItem item = req.getItems().get(i);
            ProductStock stock = stocks.get(i);
            switch (operation) {
                case DECREASE -> stock.decrease(item.getQuantity());
                case INCREASE -> stock.increase(item.getQuantity());
                case RESERVE -> stock.reserve(item.getQuantity());
                case RELEASE -> stock.release(item.getQuantity());
                case COMMIT -> stock.commit(item.getQuantity());
            }
            repo.save(stock);
        }
    }

    private Optional<StockReservation> findReservation(Long orderId) {
        if (orderId == null) {
            return Optional.empty();
        }
        return reservationRepository.findById(orderId);
    }

    private void markReservation(Long orderId, StockReservationStatus status, String message) {
        if (orderId == null) {
            return;
        }

        StockReservation reservation = reservationRepository.findById(orderId)
                .orElseGet(() -> new StockReservation(orderId, null, status, message));
        reservation.setStatus(status);
        reservation.setMessage(message);
        reservationRepository.save(reservation);
    }

    private enum StockOperation {
        DECREASE,
        INCREASE,
        RESERVE,
        RELEASE,
        COMMIT
    }
}
