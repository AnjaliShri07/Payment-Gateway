package org.paymentgateway.user.service;

import org.paymentgateway.user.entity.User;

import java.util.List;

/**
 * Common contract for domain services that support CRUD-style operations.
 *
 * @param <T> the managed entity type
 * @param <ID> the entity identifier type
 */
public interface BaseService<T, ID> {
    List<T> findAll();
    T findById(ID id);
    T save(T entity);
    T update(ID id, T entity);
    void delete(ID id);

    User findByEmail(String email);
}
