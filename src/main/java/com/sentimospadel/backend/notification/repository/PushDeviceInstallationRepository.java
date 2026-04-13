package com.sentimospadel.backend.notification.repository;

import com.sentimospadel.backend.notification.entity.PushDeviceInstallation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushDeviceInstallationRepository extends JpaRepository<PushDeviceInstallation, Long> {

    Optional<PushDeviceInstallation> findByInstallationId(String installationId);

    List<PushDeviceInstallation> findAllByUserIdAndActiveTrue(Long userId);

    Optional<PushDeviceInstallation> findByUserIdAndInstallationId(Long userId, String installationId);
}
