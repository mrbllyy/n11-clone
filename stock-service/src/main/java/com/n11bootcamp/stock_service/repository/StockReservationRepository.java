package com.n11bootcamp.stock_service.repository;

import com.n11bootcamp.stock_service.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
}
