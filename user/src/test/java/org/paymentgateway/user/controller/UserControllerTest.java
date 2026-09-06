package org.paymentgateway.user.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paymentgateway.user.DTO.BaseResponse;
import org.paymentgateway.user.client.AuthenticationServiceClient;
import org.paymentgateway.user.DTO.UserUpdateRequest;
import org.paymentgateway.user.entity.User;
import org.paymentgateway.user.service.UserServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserServiceImpl userService;

    @Mock
    private AuthenticationServiceClient authenticationServiceClient;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(userService, authenticationServiceClient);
    }

    @Test
    void getAllReturnsUsers() {
        List<User> users = List.of(new User());
        when(userService.findAll()).thenReturn(users);

        ResponseEntity<BaseResponse<List<User>>> response =
                controller.getAll("Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getBody().getData()).isSameAs(users);
    }

    @ParameterizedTest
    @CsvSource({
            "true, 200, SUCCESS",
            "false, 404, ERROR"
    })
    void getByIdReturnsExpectedStatus(boolean found, int status, String responseStatus) {
        when(userService.findById(1L)).thenReturn(found
                ? Optional.of(new User())
                : Optional.empty());

        ResponseEntity<BaseResponse<User>> response =
                controller.getById(1L, "Bearer token");

        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getBody().getStatus()).isEqualTo(responseStatus);
    }

    @ParameterizedTest
    @CsvSource({
            "true, 200, SUCCESS",
            "false, 404, ERROR"
    })
    void updateReturnsExpectedStatus(boolean updated, int status, String responseStatus) {
        Optional<User> result = updated
                ? Optional.of(new User())
                : Optional.empty();
        when(userService.update(eq(1L), any(UserUpdateRequest.class))).thenReturn(result);

        ResponseEntity<BaseResponse<User>> response =
                controller.update(1L, new UserUpdateRequest(), "Bearer token");

        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getBody().getStatus()).isEqualTo(responseStatus);
    }

    @Test
    void deleteReturnsSuccessAndDelegatesToService() {
        ResponseEntity<BaseResponse<Void>> response =
                controller.delete(1L, "Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo("SUCCESS");
        verify(userService).delete(1L);
    }

    @Test
    void fallbackMethodsReturnServiceUnavailable() {
        ResponseEntity<BaseResponse<List<User>>> allFallback =
                controller.fallbackGetAllUsers(new RuntimeException("failure"));
        ResponseEntity<BaseResponse<User>> oneFallback =
                controller.fallbackGetUserById(1L, new RuntimeException("failure"));

        assertThat(allFallback.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(oneFallback.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(allFallback.getBody().getStatus()).isEqualTo("ERROR");
        assertThat(oneFallback.getBody().getStatus()).isEqualTo("ERROR");
    }
}
