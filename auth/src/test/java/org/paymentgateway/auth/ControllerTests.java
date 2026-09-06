package org.paymentgateway.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.paymentgateway.auth.controller.AdminController;
import org.paymentgateway.auth.controller.UserController;
import org.paymentgateway.auth.dto.response.UserProfileResponse;
import org.paymentgateway.auth.exception.UnauthorizedException;
import org.paymentgateway.auth.exception.UserNotFoundException;
import org.paymentgateway.auth.security.JwtUserDetails;
import org.paymentgateway.auth.service.UserService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ControllerTests {

    @Mock
    private UserService userService;

    @Test
    void adminDashboardReturnsSuccessResponse() {
        AdminController controller = new AdminController(userService);

        var response = controller.adminDashboard();

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().success());
        assertEquals("Welcome to Admin Dashboard! Access granted.", response.getBody().message());
    }

    @Test
    void adminUsersReturnsProfiles() {
        UserProfileResponse profile = new UserProfileResponse(
            1L, "alice", "alice@example.com", List.of("ROLE_ADMIN"),
            true, null, null
        );
        when(userService.getAllUsers()).thenReturn(List.of(profile));

        var response = new AdminController(userService).getAllUsers();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of(profile), response.getBody().data());
        verify(userService).getAllUsers();
    }

    @Test
    void publicContentDoesNotRequireUser() {
        var response = new UserController(userService).publicContent();

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().success());
    }

    @Test
    void currentUserRejectsMissingAuthentication() {
        assertThrows(
            UnauthorizedException.class,
            () -> new UserController(userService).getCurrentUser(null)
        );
        verifyNoInteractions(userService);
    }

    @Test
    void currentUserLoadsProfileForAuthenticatedPrincipal() {
        JwtUserDetails details = new JwtUserDetails(
            5L, "alice", "alice@example.com", "password",
            List.of(), true, true, true, true
        );
        UserProfileResponse profile = new UserProfileResponse(
            5L, "alice", "alice@example.com", List.of(), true, null, null
        );
        when(userService.getUserProfile(5L)).thenReturn(java.util.Optional.of(profile));

        var response = new UserController(userService).getCurrentUser(details);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(profile, response.getBody().data());
    }

    @Test
    void currentUserRejectsMissingProfile() {
        JwtUserDetails details = new JwtUserDetails(
            5L, "alice", "alice@example.com", "password",
            List.of(), true, true, true, true
        );
        when(userService.getUserProfile(5L)).thenReturn(java.util.Optional.empty());

        assertThrows(
            UserNotFoundException.class,
            () -> new UserController(userService).getCurrentUser(details)
        );
    }
}
