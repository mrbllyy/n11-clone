package com.n11bootcamp.stock_service.repository;


import com.n11bootcamp.stock_service.entity.ProductStock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductStockRepository extends JpaRepository<ProductStock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ps from ProductStock ps where ps.productId = :productId")
    Optional<ProductStock> findByProductIdForUpdate(@Param("productId") Long productId);
}
