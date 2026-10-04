package org.paymentgateway.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.paymentgateway.auth.constants.AdminAccessRequestStatus;
import org.paymentgateway.auth.constants.ERole;
import org.paymentgateway.auth.entity.AdminAccessRequest;
import org.paymentgateway.auth.entity.JwtUser;
import org.paymentgateway.auth.entity.Role;
import org.paymentgateway.auth.exception.BadRequestException;
import org.paymentgateway.auth.repository.AdminAccessRequestRepository;
import org.paymentgateway.auth.repository.RoleRepository;
import org.paymentgateway.auth.repository.UserRepository;
import org.paymentgateway.auth.service.AdminAccessRequestService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAccessRequestServiceTests {

    @Mock
    private AdminAccessRequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;

    @Test
    void createsPendingRequestForRegularUser() {
        JwtUser requester = user(7L, "alice", Set.of(new Role(ERole.ROLE_USER)));
        when(requestRepository.findFirstByRequester_IdAndStatus(7L, AdminAccessRequestStatus.PENDING))
            .thenReturn(Optional.empty());
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service().createRequest(requester, "Manage team access");

        assertEquals(AdminAccessRequestStatus.PENDING, response.status());
        assertEquals("Manage team access", response.reason());
        assertEquals(7L, response.userId());
    }

    @Test
    void approvalGrantsRoleAndRecordsReviewer() {
        JwtUser requester = user(7L, "alice", Set.of(new Role(ERole.ROLE_USER)));
        JwtUser reviewer = user(9L, "admin", Set.of(new Role(ERole.ROLE_ADMIN)));
        AdminAccessRequest request = request(requester);
        when(requestRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(request));
        when(userRepository.findById(9L)).thenReturn(Optional.of(reviewer));
        when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(new Role(ERole.ROLE_ADMIN)));
        when(requestRepository.save(request)).thenReturn(request);

        var response = service().decideRequest(11L, 9L, AdminAccessRequestStatus.APPROVED, "Approved");

        assertEquals(AdminAccessRequestStatus.APPROVED, response.status());
        assertTrue(requester.getRoles().stream().anyMatch(role -> role.getName() == ERole.ROLE_ADMIN));
        assertEquals(reviewer, request.getReviewer());
        assertNotNull(request.getReviewedAt());
        verify(userRepository).save(requester);
    }

    @Test
    void rejectsRequestWithoutGrantingAdminRole() {
        JwtUser requester = user(7L, "alice", Set.of(new Role(ERole.ROLE_USER)));
        JwtUser reviewer = user(9L, "admin", Set.of(new Role(ERole.ROLE_ADMIN)));
        AdminAccessRequest request = request(requester);
        when(requestRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(request));
        when(userRepository.findById(9L)).thenReturn(Optional.of(reviewer));
        when(requestRepository.save(request)).thenReturn(request);

        var response = service().decideRequest(11L, 9L, AdminAccessRequestStatus.REJECTED, null);

        assertEquals(AdminAccessRequestStatus.REJECTED, response.status());
        assertFalse(requester.getRoles().stream().anyMatch(role -> role.getName() == ERole.ROLE_ADMIN));
        verify(userRepository, never()).save(requester);
    }

    @Test
    void doesNotAllowPendingAsAnAdminDecision() {
        assertThrows(
            BadRequestException.class,
            () -> service().decideRequest(11L, 9L, AdminAccessRequestStatus.PENDING, null)
        );
        verifyNoInteractions(requestRepository, userRepository, roleRepository);
    }

    private AdminAccessRequestService service() {
        return new AdminAccessRequestService(requestRepository, userRepository, roleRepository);
    }

    private static JwtUser user(Long id, String username, Set<Role> roles) {
        JwtUser user = new JwtUser();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setRoles(roles);
        return user;
    }

    private static AdminAccessRequest request(JwtUser requester) {
        AdminAccessRequest request = new AdminAccessRequest();
        request.setRequester(requester);
        request.setReason("Manage team access");
        request.setStatus(AdminAccessRequestStatus.PENDING);
        return request;
    }
}
