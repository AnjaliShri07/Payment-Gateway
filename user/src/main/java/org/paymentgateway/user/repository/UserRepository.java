package org.paymentgateway.user.repository;

import org.paymentgateway.user.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public interface  UserRepository extends BaseRepository<User, Long> {
}
