package com.sentimospadel.backend.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.account.dto.AccountDeletionRequest;
import com.sentimospadel.backend.account.dto.AccountDeletionResponse;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import com.sentimospadel.backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    @Mock
    private UserRepository userRepository;

    private AccountDeletionService accountDeletionService;

    @BeforeEach
    void setUp() {
        accountDeletionService = new AccountDeletionService(userRepository);
    }

    @Test
    void getDeletionRequestReturnsEmptyStateWhenUserHasNotRequestedIt() {
        User user = user();

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));

        AccountDeletionResponse response = accountDeletionService.getDeletionRequest("PLAYER@example.com ");

        assertThat(response.requested()).isFalse();
        assertThat(response.requestedAt()).isNull();
    }

    @Test
    void requestDeletionPersistsTimestampAndTrimmedReasonWithoutChangingStatus() {
        User user = user();

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        AccountDeletionResponse response = accountDeletionService.requestDeletion(
                "player@example.com",
                new AccountDeletionRequest("  Quiero cerrar la cuenta  ")
        );

        verify(userRepository).saveAndFlush(user);
        assertThat(response.requested()).isTrue();
        assertThat(response.requestedAt()).isNotNull();
        assertThat(user.getAccountDeletionReason()).isEqualTo("Quiero cerrar la cuenta");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void requestDeletionKeepsOriginalTimestampWhenRepeated() {
        Instant originalRequestedAt = Instant.parse("2026-04-13T10:00:00Z");
        User user = user();
        user.setAccountDeletionRequestedAt(originalRequestedAt);

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        AccountDeletionResponse response = accountDeletionService.requestDeletion(
                "player@example.com",
                new AccountDeletionRequest(null)
        );

        assertThat(response.requestedAt()).isEqualTo(originalRequestedAt);
        assertThat(user.getAccountDeletionRequestedAt()).isEqualTo(originalRequestedAt);
    }

    private User user() {
        return User.builder()
                .id(10L)
                .email("player@example.com")
                .phone("091234567")
                .passwordHash("hash")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .activityTrackingEnabled(true)
                .operationalNotificationsEnabled(true)
                .build();
    }
}
