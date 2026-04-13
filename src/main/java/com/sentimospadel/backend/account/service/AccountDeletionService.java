package com.sentimospadel.backend.account.service;

import com.sentimospadel.backend.account.dto.AccountDeletionRequest;
import com.sentimospadel.backend.account.dto.AccountDeletionResponse;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private static final String REQUESTED_MESSAGE = "Solicitud de eliminacion de cuenta recibida.";
    private static final String NOT_REQUESTED_MESSAGE = "No hay solicitud de eliminacion de cuenta registrada.";

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AccountDeletionResponse getDeletionRequest(String email) {
        User user = requireUser(email);
        return toResponse(user);
    }

    @Transactional
    public AccountDeletionResponse requestDeletion(String email, AccountDeletionRequest request) {
        User user = requireUser(email);

        if (user.getAccountDeletionRequestedAt() == null) {
            user.setAccountDeletionRequestedAt(Instant.now());
        }

        String reason = request == null ? null : normalizeReason(request.reason());
        user.setAccountDeletionReason(reason);

        return toResponse(userRepository.saveAndFlush(user));
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " was not found"));
    }

    private AccountDeletionResponse toResponse(User user) {
        Instant requestedAt = user.getAccountDeletionRequestedAt();
        return new AccountDeletionResponse(
                requestedAt != null,
                requestedAt,
                requestedAt == null ? NOT_REQUESTED_MESSAGE : REQUESTED_MESSAGE
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String normalized = reason.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
