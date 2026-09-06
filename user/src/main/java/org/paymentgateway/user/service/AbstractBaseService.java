package org.paymentgateway.user.service;

import org.paymentgateway.user.repository.BaseRepository;

import java.util.List;
import java.util.Optional;

/**
 * Shared base implementation for service classes that expose default CRUD operations.
 *
 * @param <T> the entity type managed by the service
 * @param <ID> the identifier type for the entity
 */
public abstract class AbstractBaseService<T, ID> implements BaseService<T, ID> {

    protected final BaseRepository<T, ID> baseRepository;

    protected AbstractBaseService(BaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public List<T> findAll() {
        return baseRepository.findAll();
    }

    @Override
    public Optional<T> findById(ID id) {
        return baseRepository.findById(id);
    }

    @Override
    public T save(T entity) {
        return baseRepository.save(entity);
    }

    @Override
    public void delete(ID id) {
        baseRepository.deleteById(id);
    }

    // update left abstract — subclasses can override with entity-specific logic
    @Override
    public abstract Optional<T> update(ID id, T entity);
}
