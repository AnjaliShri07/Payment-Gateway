package org.paymentgateway.user.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void genericExceptionReturnsInternalServerError() {
        var response = handler.handleException(new RuntimeException("failure"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo("ERROR");
        assertThat(response.getBody().getMessage()).contains("failure");
    }

    @Test
    void resourceNotFoundReturnsNotFound() {
        var response = handler.handleResourceNotFound(
                new ResourceNotFoundException("User not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("User not found");
    }

    @Test
    void validationExceptionReturnsFieldErrors() {
        BindingResult bindingResult = org.mockito.Mockito.mock(BindingResult.class);
        MethodArgumentNotValidException exception =
                org.mockito.Mockito.mock(MethodArgumentNotValidException.class);
        FieldError fieldError = new FieldError(
                "user", "email", "must be a valid email");
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));

        var response = handler.handleValidationErrors(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getData())
                .containsEntry("email", "must be a valid email");
    }
}
