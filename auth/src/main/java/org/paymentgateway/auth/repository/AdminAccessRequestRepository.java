package org.paymentgateway.auth.repository;

import org.paymentgateway.auth.constants.AdminAccessRequestStatus;
import org.paymentgateway.auth.entity.AdminAccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface AdminAccessRequestRepository extends JpaRepository<AdminAccessRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from AdminAccessRequest request where request.id = :requestId")
    Optional<AdminAccessRequest> findByIdForUpdate(@Param("requestId") Long requestId);

    List<AdminAccessRequest> findByStatusOrderByCreatedAtAsc(AdminAccessRequestStatus status);

    Optional<AdminAccessRequest> findFirstByRequester_IdAndStatus(
        Long requesterId,
        AdminAccessRequestStatus status
    );

    Optional<AdminAccessRequest> findTopByRequester_IdOrderByCreatedAtDesc(Long requesterId);
}
