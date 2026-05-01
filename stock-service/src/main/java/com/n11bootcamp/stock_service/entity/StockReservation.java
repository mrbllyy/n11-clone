package com.n11bootcamp.stock_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "stock_reservation")
public class StockReservation {

    @Id
    private Long orderId;

    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockReservationStatus status;

    private String message;
    private Instant createdAt;
    private Instant updatedAt;

    public StockReservation() {
    }

    public StockReservation(Long orderId, String username, StockReservationStatus status, String message) {
        this.orderId = orderId;
        this.username = username;
        this.status = status;
        this.message = message;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public StockReservationStatus getStatus() { return status; }
    public void setStatus(StockReservationStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getMessage() { return message; }
    public void setMessage(String message) {
        this.message = message;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
