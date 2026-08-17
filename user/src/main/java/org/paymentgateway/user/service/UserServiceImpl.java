package org.paymentgateway.user.service;

import lombok.extern.slf4j.Slf4j;
import org.paymentgateway.user.entity.User;
import org.paymentgateway.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl extends AbstractBaseService<User, Long> {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        super(userRepository);
        this.userRepository = userRepository;
    }

    @Override
    public User update(Long id, User user) {
        User existing = userRepository.findById(id).orElse(null);
        if (existing != null) {
            existing.setUsername(user.getUsername());
            existing.setEmail(user.getEmail());
            existing.setPassword(user.getPassword());
            existing.setPhone(user.getPhone());
            existing.setAddress(user.getAddress());
            existing.setRole(user.getRole());
            return userRepository.save(existing);
        }
        return null;
    }

    @Override
    public User findByEmail(String email) {
        return null;
    }
}
