package org.paymentgateway.auth.repository;

import jakarta.persistence.LockModeType;
import org.paymentgateway.auth.entity.JwtUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
/**
 * Repository interface for authentication domain persistence.
 */
public interface UserRepository extends JpaRepository<JwtUser, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from JwtUser u where u.id = :userId")
    Optional<JwtUser> findByIdForUpdate(@Param("userId") Long userId);

    Optional<JwtUser> findByUsername(String username);

    Optional<JwtUser> findByEmail(String email);

    Optional<JwtUser> findByUsernameOrEmail(String username, String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
}
