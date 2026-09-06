package org.paymentgateway.auth.repository;

import org.paymentgateway.auth.entity.JwtUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<JwtUser, Long> {

    Optional<JwtUser> findByUsername(String username);

    Optional<JwtUser> findByEmail(String email);

    Optional<JwtUser> findByUsernameOrEmail(String username, String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
}
