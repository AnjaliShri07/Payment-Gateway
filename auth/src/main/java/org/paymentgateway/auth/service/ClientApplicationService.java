package org.paymentgateway.auth.service;

import org.paymentgateway.auth.dto.request.ClientRegisterRequest;
import org.paymentgateway.auth.dto.request.ClientTokenRequest;
import org.paymentgateway.auth.dto.response.ClientRegisterResponse;
import org.paymentgateway.auth.dto.response.ClientTokenResponse;
import org.paymentgateway.auth.entity.ClientApplication;
import org.paymentgateway.auth.security.JwtUserDetails;
import org.paymentgateway.auth.repository.ClientApplicationRepository;
import org.paymentgateway.auth.security.jwt.JwtTokenProvider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ClientApplicationService {

    private final ClientApplicationRepository clientRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    public ClientApplicationService(
        ClientApplicationRepository clientRepo,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.clientRepo = clientRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public ClientRegisterResponse registerClient(ClientRegisterRequest req) {
        String clientId = "client-" + UUID.randomUUID();
        byte[] secretBytes = new byte[48];
        secureRandom.nextBytes(secretBytes);
        String clientSecretPlain = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        String hashed = passwordEncoder.encode(clientSecretPlain);

        ClientApplication app = new ClientApplication();
        app.setClientId(clientId);
        app.setClientSecret(hashed);
        app.setName(req.name());
        app.setScopes(req.scopes());

        clientRepo.save(app);

        return new ClientRegisterResponse(clientId, clientSecretPlain);
    }

    public ClientTokenResponse issueToken(ClientTokenRequest req) {
        // basic client credentials validation
        ClientApplication app = clientRepo.findByClientId(req.clientId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid client credentials"));

        if (!passwordEncoder.matches(req.clientSecret(), app.getClientSecret())) {
            throw new IllegalArgumentException("Invalid client credentials");
        }

        Set<String> scopes = app.getScopes();
        List<SimpleGrantedAuthority> authorities = (scopes != null)
            ? scopes.stream().map(s -> new SimpleGrantedAuthority("SCOPE_" + s)).collect(Collectors.toList())
            : List.of();

        JwtUserDetails userDetails = new JwtUserDetails(
            app.getId(),
            app.getClientId(),
            app.getName() + "@clients",
            "", // no password retained in details
            authorities,
            true,
            true,
            true,
            true
        );

        String accessToken = jwtTokenProvider.generateTokenFromUserDetails(userDetails);
        long expiresIn = jwtTokenProvider.getExpirationMs() / 1000;
        String scopeStr = (scopes != null) ? String.join(" ", scopes) : "";

        return new ClientTokenResponse(accessToken, "Bearer", expiresIn, scopeStr);
    }
}
