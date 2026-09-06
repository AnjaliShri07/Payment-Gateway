package org.paymentgateway.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.paymentgateway.user.DTO.UserUpdateRequest;
import org.paymentgateway.user.entity.User;
import org.paymentgateway.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository);
    }

    @Test
    void findAllDelegatesToRepository() {
        List<User> users = List.of(new User());
        when(userRepository.findAll()).thenReturn(users);

        assertThat(userService.findAll()).isSameAs(users);
        verify(userRepository).findAll();
    }

    @Test
    void findByIdReturnsUserWhenPresent() {
        User user = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userService.findById(1L)).containsSame(user);
    }

    @Test
    void findByIdReturnsNullWhenMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(userService.findById(1L)).isEmpty();
    }

    @Test
    void saveDelegatesToRepository() {
        User user = new User();
        when(userRepository.save(user)).thenReturn(user);

        assertThat(userService.save(user)).isSameAs(user);
        verify(userRepository).save(user);
    }

    @Test
    void deleteDelegatesToRepository() {
        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void updateEntityChangesOnlySuppliedFields() {
        User existing = new User();
        existing.setUsername("old-user");
        existing.setEmail("old@example.com");
        existing.setPassword("old-password");
        User update = new User();
        update.setUsername("new-user");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        Optional<User> result = userService.update(1L, update);

        assertThat(result).containsSame(existing);
        assertThat(existing.getUsername()).isEqualTo("new-user");
        assertThat(existing.getEmail()).isEqualTo("old@example.com");
        assertThat(existing.getPassword()).isEqualTo("old-password");
        verify(userRepository).save(existing);
    }

    @Test
    void updateRequestChangesBooleanFieldsAndRoles() {
        User existing = new User();
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEnabled(false);
        request.setAccountNonExpired(false);
        request.setCredentialsNonExpired(false);
        request.setAccountNonLocked(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        userService.update(1L, request);

        assertThat(existing.isEnabled()).isFalse();
        assertThat(existing.isAccountNonExpired()).isFalse();
        assertThat(existing.isCredentialsNonExpired()).isFalse();
        assertThat(existing.isAccountNonLocked()).isFalse();
        verify(userRepository).save(existing);
    }

    @Test
    void updateReturnsNullAndDoesNotSaveWhenUserIsMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(userService.update(1L, new UserUpdateRequest())).isEmpty();

        verify(userRepository, never()).save(any());
    }
}
