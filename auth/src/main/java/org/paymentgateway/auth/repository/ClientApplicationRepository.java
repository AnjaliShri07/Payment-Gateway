package org.paymentgateway.auth.repository;

import org.paymentgateway.auth.entity.ClientApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientApplicationRepository extends JpaRepository<ClientApplication, Long> {
    Optional<ClientApplication> findByClientId(String clientId);
}
