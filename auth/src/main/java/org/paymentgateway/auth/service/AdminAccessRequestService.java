package org.paymentgateway.auth.service;

import org.paymentgateway.auth.constants.AdminAccessRequestStatus;
import org.paymentgateway.auth.constants.ERole;
import org.paymentgateway.auth.dto.response.AdminAccessRequestResponse;
import org.paymentgateway.auth.entity.AdminAccessRequest;
import org.paymentgateway.auth.entity.JwtUser;
import org.paymentgateway.auth.entity.Role;
import org.paymentgateway.auth.exception.BadRequestException;
import org.paymentgateway.auth.exception.ResourceNotFoundException;
import org.paymentgateway.auth.repository.AdminAccessRequestRepository;
import org.paymentgateway.auth.repository.RoleRepository;
import org.paymentgateway.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminAccessRequestService {

    private final AdminAccessRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminAccessRequestService(
        AdminAccessRequestRepository requestRepository,
        UserRepository userRepository,
        RoleRepository roleRepository
    ) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public AdminAccessRequestResponse createRequest(JwtUser requester, String reason) {
        if (requester == null || requester.getId() == null) {
            throw new BadRequestException("An account is required to request administrator access");
        }
        if (!StringUtils.hasText(reason) || reason.trim().length() > 500) {
            throw new BadRequestException("A reason of 1 to 500 characters is required");
        }
        if (requester.getRoles() != null && requester.getRoles().stream()
            .anyMatch(role -> role.getName() == ERole.ROLE_ADMIN)) {
            throw new BadRequestException("This account already has administrator access");
        }
        if (requestRepository.findFirstByRequester_IdAndStatus(
            requester.getId(),
            AdminAccessRequestStatus.PENDING
        ).isPresent()) {
            throw new BadRequestException("An administrator access request is already pending");
        }

        AdminAccessRequest request = new AdminAccessRequest();
        request.setRequester(requester);
        request.setReason(reason.trim());
        request.setStatus(AdminAccessRequestStatus.PENDING);
        return toResponse(requestRepository.save(request));
    }

    @Transactional
    public AdminAccessRequestResponse createRequest(Long requesterId, String reason) {
        JwtUser requester = userRepository.findByIdForUpdate(requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Requester account was not found"));
        return createRequest(requester, reason);
    }

    @Transactional(readOnly = true)
    public List<AdminAccessRequestResponse> getPendingRequests() {
        return requestRepository.findByStatusOrderByCreatedAtAsc(AdminAccessRequestStatus.PENDING)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public AdminAccessRequestResponse getLatestRequest(Long userId) {
        return requestRepository.findTopByRequester_IdOrderByCreatedAtDesc(userId)
            .map(this::toResponse)
            .orElse(null);
    }

    @Transactional
    public AdminAccessRequestResponse decideRequest(
        Long requestId,
        Long reviewerId,
        AdminAccessRequestStatus decision,
        String note
    ) {
        if (decision == null || decision == AdminAccessRequestStatus.PENDING) {
            throw new BadRequestException("The decision must be APPROVED or REJECTED");
        }

        AdminAccessRequest request = requestRepository.findByIdForUpdate(requestId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Administrator access request " + requestId + " was not found"
            ));
        if (request.getStatus() != AdminAccessRequestStatus.PENDING) {
            throw new BadRequestException("This administrator access request has already been reviewed");
        }

        JwtUser reviewer = userRepository.findById(reviewerId)
            .orElseThrow(() -> new ResourceNotFoundException("Reviewer account was not found"));

        if (decision == AdminAccessRequestStatus.APPROVED) {
            JwtUser requester = request.getRequester();
            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_ADMIN is not found"));
            Set<Role> roles = requester.getRoles() == null
                ? new HashSet<>()
                : new HashSet<>(requester.getRoles());
            roles.add(adminRole);
            requester.setRoles(roles);
            userRepository.save(requester);
        }

        request.setStatus(decision);
        request.setReviewer(reviewer);
        request.setReviewedAt(Instant.now());
        request.setDecisionNote(StringUtils.hasText(note) ? note.trim() : null);
        return toResponse(requestRepository.save(request));
    }

    private AdminAccessRequestResponse toResponse(AdminAccessRequest request) {
        JwtUser requester = request.getRequester();
        return new AdminAccessRequestResponse(
            request.getId(),
            requester.getId(),
            requester.getUsername(),
            requester.getEmail(),
            request.getReason(),
            request.getStatus(),
            request.getCreatedAt(),
            request.getReviewedAt(),
            request.getReviewer() != null ? request.getReviewer().getUsername() : null,
            request.getDecisionNote()
        );
    }
}
