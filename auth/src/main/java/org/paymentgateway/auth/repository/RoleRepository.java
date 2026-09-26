package org.paymentgateway.auth.repository;

import org.paymentgateway.auth.constants.ERole;
import org.paymentgateway.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
/**
 * Repository interface for authentication domain persistence.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(ERole name);
}
