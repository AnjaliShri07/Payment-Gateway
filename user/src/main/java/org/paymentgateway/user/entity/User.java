package org.paymentgateway.user.entity;

import jakarta.persistence.*;
import org.paymentgateway.user.constant.UserRole;
import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a User entity in the payment gateway system.
 * <p>
 * This entity stores user details such as username, email, phone,
 * address, role, and audit timestamps. Passwords must always be stored
 * securely in hashed form.
 * </p>
 */
@Entity
@Table(name = "users")
public class User {

        private static final long serialVersionUID = 1L;

    /** Primary key identifier for the user. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @JsonProperty("id")
    private Long id;

    /** Unique username chosen by the user. */
    @Column(name = "username", nullable = false)
    @JsonProperty("username")
    private String username;

    /** Unique email address of the user. */
    @Column(name = "email", nullable = false, unique = true)
    @JsonProperty("email")
    private String email;

    /** Securely stored password (hashed). */
    @Column(name = "password", nullable = false)
    @JsonProperty("password")
    private String password;

    /** Contact phone number of the user. */
    @Column(name = "phone", nullable = true)
    @JsonProperty("phone")
    private String phone;

    /** Residential or mailing address of the user. */
    @Column(name = "address", nullable = true)
    @JsonProperty("address")
    private String address;

    /** Role assigned to the user (CUSTOMER or ADMIN). */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @JsonProperty("role")
    private UserRole role;

    /** Timestamp when the user record was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private Timestamp created_at;

    /** Timestamp when the user record was last updated. */
    @Column(name = "updated_at", nullable = false)
    @JsonProperty("updated_at")
    private Timestamp updated_at;

    // Relationship with payments
    //@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    //private List<Payment> payments;

    // getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(Timestamp updated_at) {
        this.updated_at = updated_at;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }
}

