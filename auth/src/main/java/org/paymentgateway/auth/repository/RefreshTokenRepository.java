package org.paymentgateway.auth.repository;

import org.paymentgateway.auth.entity.RefreshToken;
import org.paymentgateway.auth.entity.JwtUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUser(JwtUser user);

    @Modifying
    int deleteByUser(JwtUser user);
}
