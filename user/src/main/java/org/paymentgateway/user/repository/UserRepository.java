package org.paymentgateway.user.repository;

import org.paymentgateway.user.entity.User;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for persisting and retrieving {@link User} entities.
 */
@Repository
public interface  UserRepository extends BaseRepository<User, Long> {
}
