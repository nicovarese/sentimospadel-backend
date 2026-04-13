package com.sentimospadel.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.notification.dto.PushDeviceRegistrationRequest;
import com.sentimospadel.backend.notification.dto.PushDeviceResponse;
import com.sentimospadel.backend.notification.dto.PushDeviceUnregisterRequest;
import com.sentimospadel.backend.notification.entity.PushDeviceInstallation;
import com.sentimospadel.backend.notification.enums.PushDevicePlatform;
import com.sentimospadel.backend.notification.repository.PushDeviceInstallationRepository;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import com.sentimospadel.backend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushDeviceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PushDeviceInstallationRepository pushDeviceInstallationRepository;

    private PushDeviceService pushDeviceService;

    @BeforeEach
    void setUp() {
        pushDeviceService = new PushDeviceService(userRepository, pushDeviceInstallationRepository);
    }

    @Test
    void registerDeviceTakesOverExistingInstallationForAuthenticatedUser() {
        User player = user(10L, "player@example.com");
        PushDeviceInstallation installation = PushDeviceInstallation.builder()
                .id(33L)
                .installationId("ios-install-1")
                .platform(PushDevicePlatform.IOS)
                .pushToken("old-token")
                .active(false)
                .build();

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(player));
        when(pushDeviceInstallationRepository.findByInstallationId("ios-install-1")).thenReturn(Optional.of(installation));
        when(pushDeviceInstallationRepository.saveAndFlush(installation)).thenReturn(installation);

        PushDeviceResponse response = pushDeviceService.registerDevice(
                "player@example.com",
                new PushDeviceRegistrationRequest("ios-install-1", PushDevicePlatform.IOS, "new-token")
        );

        assertThat(installation.getUser()).isEqualTo(player);
        assertThat(installation.getPushToken()).isEqualTo("new-token");
        assertThat(installation.isActive()).isTrue();
        assertThat(installation.getLastSeenAt()).isNotNull();
        assertThat(response.installationId()).isEqualTo("ios-install-1");
        assertThat(response.active()).isTrue();
    }

    @Test
    void unregisterDeviceMarksInstallationInactiveForAuthenticatedUser() {
        User player = user(10L, "player@example.com");
        PushDeviceInstallation installation = PushDeviceInstallation.builder()
                .id(33L)
                .user(player)
                .installationId("android-install-1")
                .platform(PushDevicePlatform.ANDROID)
                .pushToken("token")
                .active(true)
                .build();

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(player));
        when(pushDeviceInstallationRepository.findByUserIdAndInstallationId(10L, "android-install-1"))
                .thenReturn(Optional.of(installation));
        when(pushDeviceInstallationRepository.saveAndFlush(installation)).thenReturn(installation);

        PushDeviceResponse response = pushDeviceService.unregisterDevice(
                "player@example.com",
                new PushDeviceUnregisterRequest("android-install-1")
        );

        assertThat(installation.isActive()).isFalse();
        assertThat(installation.getLastSeenAt()).isNotNull();
        assertThat(response.active()).isFalse();
    }

    private User user(Long id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .phone("091234567")
                .passwordHash("hash")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
