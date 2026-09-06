package org.paymentgateway.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.paymentgateway.user.entity.Role;

import java.util.Set;

/**
 * Fields that may be changed when updating a user.
 * Null fields are left unchanged.
 */
@Getter
@Setter
public class UserUpdateRequest {

    @Size(min = 3, max = 50)
    private String username;

    @Size(max = 100)
    @Email
    private String email;

    @Size(max = 120)
    private String password;

    private Set<Role> roles;
    private Boolean enabled;
    private Boolean accountNonExpired;
    private Boolean credentialsNonExpired;
    private Boolean accountNonLocked;
}
