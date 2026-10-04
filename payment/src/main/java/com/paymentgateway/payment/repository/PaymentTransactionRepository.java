package com.paymentgateway.payment.repository;

import com.paymentgateway.payment.entity.PaymentTransaction;
import com.paymentgateway.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, String> {

    List<PaymentTransaction> findByUserId(Long userId);

    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PaymentTransaction> findByStatus(PaymentStatus status);

    List<PaymentTransaction> findByUserIdAndStatus(Long userId, PaymentStatus status);

    List<PaymentTransaction> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PaymentStatus status);

    Optional<PaymentTransaction> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    long countByUserIdAndCreatedAtAfter(Long userId, Instant after);

    long countByStatus(PaymentStatus status);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, PaymentStatus status);

    @Query("SELECT SUM(t.amount) FROM PaymentTransaction t WHERE t.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT SUM(t.amount) FROM PaymentTransaction t WHERE t.userId = :userId AND t.status = :status")
    BigDecimal sumAmountByUserIdAndStatus(@Param("userId") Long userId, @Param("status") PaymentStatus status);
}
