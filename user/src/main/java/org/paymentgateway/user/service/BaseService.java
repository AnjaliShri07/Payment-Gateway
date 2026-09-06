package org.paymentgateway.user.service;

import org.paymentgateway.user.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * Common contract for domain services that support CRUD-style operations.
 *
 * @param <T> the managed entity type
 * @param <ID> the entity identifier type
 */
public interface BaseService<T, ID> {
    List<T> findAll();
    Optional<T> findById(ID id);
    T save(T entity);
    Optional<T> update(ID id, T entity);
    void delete(ID id);

    Optional<User> findByEmail(String email);
}
