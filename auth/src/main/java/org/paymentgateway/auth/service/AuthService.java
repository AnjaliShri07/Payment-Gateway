package org.paymentgateway.auth.service;

import org.paymentgateway.auth.constants.ERole;
import org.paymentgateway.auth.dto.request.LoginRequest;
import org.paymentgateway.auth.dto.request.LogoutRequest;
import org.paymentgateway.auth.dto.request.RegisterRequest;
import org.paymentgateway.auth.dto.request.TokenRefreshRequest;
import org.paymentgateway.auth.dto.response.AuthResponse;
import org.paymentgateway.auth.dto.response.TokenRefreshResponse;
import org.paymentgateway.auth.entity.RefreshToken;
import org.paymentgateway.auth.entity.Role;
import org.paymentgateway.auth.entity.User;
import org.paymentgateway.auth.exception.*;
import org.paymentgateway.auth.repository.RoleRepository;
import org.paymentgateway.auth.repository.UserRepository;
import org.paymentgateway.auth.security.CustomUserDetails;
import org.paymentgateway.auth.security.jwt.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
        AuthenticationManager authenticationManager,
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider,
        RefreshTokenService refreshTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (registerRequest == null) {
            throw new BadRequestException("Registration request payload cannot be null");
        }

        if (userRepository.existsByUsername(registerRequest.username())) {
            throw new UserAlreadyExistsException("Error: Username '" + registerRequest.username() + "' is already taken!");
        }

        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new UserAlreadyExistsException("Error: Email '" + registerRequest.email() + "' is already in use!");
        }

        User user = new User(
            registerRequest.username().trim(),
            registerRequest.email().trim().toLowerCase(),
            passwordEncoder.encode(registerRequest.password())
        );

        Set<String> strRoles = registerRequest.roles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Error: Role ROLE_USER is not found."));
            roles.add(userRole);
        } else {
            for (String roleStr : strRoles) {
                if (!StringUtils.hasText(roleStr)) {
                    continue;
                }
                String normalizedRole = roleStr.trim().toUpperCase();
                switch (normalizedRole) {
                    case "ADMIN", "ROLE_ADMIN" -> {
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                            .orElseThrow(() -> new ResourceNotFoundException("Error: Role ROLE_ADMIN is not found."));
                        roles.add(adminRole);
                    }
                    case "MOD", "MODERATOR", "ROLE_MODERATOR" -> {
                        Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
                            .orElseThrow(() -> new ResourceNotFoundException("Error: Role ROLE_MODERATOR is not found."));
                        roles.add(modRole);
                    }
                    case "USER", "ROLE_USER" -> {
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                            .orElseThrow(() -> new ResourceNotFoundException("Error: Role ROLE_USER is not found."));
                        roles.add(userRole);
                    }
                    default -> throw new InvalidRoleException("Error: Role '" + roleStr + "' is invalid. Allowed roles: USER, MODERATOR, ADMIN");
                }
            }
            if (roles.isEmpty()) {
                Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new ResourceNotFoundException("Error: Role ROLE_USER is not found."));
                roles.add(userRole);
            }
        }

        user.setRoles(roles);
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        if (loginRequest == null) {
            throw new BadRequestException("Login request cannot be null");
        }

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.usernameOrEmail().trim(),
                loginRequest.password()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String jwt = jwtTokenProvider.generateToken(authentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        List<String> roles = userDetails.getAuthorities() != null
            ? userDetails.getAuthorities().stream()
                .filter(Objects::nonNull)
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList())
            : List.of();

        return AuthResponse.of(
            jwt,
            refreshToken.getToken(),
            jwtTokenProvider.getExpirationMs() / 1000,
            userDetails.getId(),
            userDetails.getUsername(),
            userDetails.getEmail(),
            roles
        );
    }

    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        if (request == null || !StringUtils.hasText(request.refreshToken())) {
            throw new BadRequestException("Refresh token cannot be blank");
        }

        String requestRefreshToken = request.refreshToken().trim();

        return refreshTokenService.findByToken(requestRefreshToken)
            .map(refreshTokenService::verifyExpiration)
            .map(refreshTokenService::rotateRefreshToken)
            .map(token -> {
                User user = token.getUser();
                if (user == null) {
                    throw new UserNotFoundException("User associated with refresh token no longer exists");
                }
                CustomUserDetails userDetails = CustomUserDetails.build(user);
                String newAccessToken = jwtTokenProvider.generateTokenFromUserDetails(userDetails);

                return TokenRefreshResponse.of(
                    newAccessToken,
                    token.getToken(),
                    jwtTokenProvider.getExpirationMs() / 1000
                );
            })
            .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token is not found in database!"));
    }

    @Transactional
    public void logout(LogoutRequest logoutRequest) {
        if (logoutRequest != null && StringUtils.hasText(logoutRequest.refreshToken())) {
            refreshTokenService.revokeRefreshToken(logoutRequest.refreshToken().trim());
        }
        SecurityContextHolder.clearContext();
    }
}
