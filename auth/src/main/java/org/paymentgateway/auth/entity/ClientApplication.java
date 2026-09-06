package org.paymentgateway.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "client_applications", uniqueConstraints = {@UniqueConstraint(columnNames = "client_id")})
public class ClientApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, length = 100, unique = true)
    private String clientId;

    @Column(name = "client_secret", nullable = false, length = 200)
    private String clientSecret; // hashed

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_scopes", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "scope")
    private Set<String> scopes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

}
