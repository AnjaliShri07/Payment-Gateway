package org.paymentgateway.auth.security;

import org.paymentgateway.auth.entity.User;
import org.paymentgateway.auth.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        if (!StringUtils.hasText(usernameOrEmail)) {
            throw new UsernameNotFoundException("Username or email parameter cannot be blank");
        }

        String searchKey = usernameOrEmail.trim();

        User user = userRepository.findByUsernameOrEmail(searchKey, searchKey)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + searchKey));

        return CustomUserDetails.build(user);
    }
}
