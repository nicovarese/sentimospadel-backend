package com.sentimospadel.backend.notification.service;

import com.sentimospadel.backend.notification.dto.PushDeviceRegistrationRequest;
import com.sentimospadel.backend.notification.dto.PushDeviceResponse;
import com.sentimospadel.backend.notification.dto.PushDeviceUnregisterRequest;
import com.sentimospadel.backend.notification.entity.PushDeviceInstallation;
import com.sentimospadel.backend.notification.repository.PushDeviceInstallationRepository;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushDeviceService {

    private final UserRepository userRepository;
    private final PushDeviceInstallationRepository pushDeviceInstallationRepository;

    @Transactional
    public PushDeviceResponse registerDevice(String email, PushDeviceRegistrationRequest request) {
        User user = requireUser(email);
        Instant now = Instant.now();
        String installationId = request.installationId().trim();
        String pushToken = request.pushToken().trim();

        PushDeviceInstallation installation = pushDeviceInstallationRepository.findByInstallationId(installationId)
                .orElseGet(() -> PushDeviceInstallation.builder()
                        .installationId(installationId)
                        .build());

        installation.setUser(user);
        installation.setPlatform(request.platform());
        installation.setPushToken(pushToken);
        installation.setActive(true);
        installation.setLastSeenAt(now);

        return toResponse(pushDeviceInstallationRepository.saveAndFlush(installation));
    }

    @Transactional
    public PushDeviceResponse unregisterDevice(String email, PushDeviceUnregisterRequest request) {
        User user = requireUser(email);
        PushDeviceInstallation installation = pushDeviceInstallationRepository.findByUserIdAndInstallationId(
                        user.getId(),
                        request.installationId().trim()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Push device installation was not found"));

        installation.setActive(false);
        installation.setLastSeenAt(Instant.now());

        return toResponse(pushDeviceInstallationRepository.saveAndFlush(installation));
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " was not found"));
    }

    private PushDeviceResponse toResponse(PushDeviceInstallation installation) {
        return new PushDeviceResponse(
                installation.getInstallationId(),
                installation.getPlatform(),
                installation.isActive(),
                installation.getLastSeenAt(),
                installation.getCreatedAt(),
                installation.getUpdatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
