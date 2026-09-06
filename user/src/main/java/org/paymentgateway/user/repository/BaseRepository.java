package org.paymentgateway.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Base repository type for application entities using Spring Data JPA.
 *
 * @param <T> the entity type managed by the repository
 * @param <ID> the primary key type
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
    // Common repository methods can be declared here if needed
}
