package com.sentimospadel.backend.verification.repository;

import com.sentimospadel.backend.verification.entity.ClubVerificationRequest;
import com.sentimospadel.backend.verification.enums.ClubVerificationRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubVerificationRequestRepository extends JpaRepository<ClubVerificationRequest, Long> {

    @Override
    @EntityGraph(attributePaths = {"playerProfile", "playerProfile.user", "club", "reviewedByUser"})
    Optional<ClubVerificationRequest> findById(Long id);

    @EntityGraph(attributePaths = {"playerProfile", "playerProfile.user", "club", "reviewedByUser"})
    List<ClubVerificationRequest> findTop5ByPlayerProfileIdOrderByCreatedAtDesc(Long playerProfileId);

    @EntityGraph(attributePaths = {"playerProfile", "playerProfile.user", "club", "reviewedByUser"})
    List<ClubVerificationRequest> findAllByClubIdOrderByCreatedAtDesc(Long clubId);

    boolean existsByPlayerProfileIdAndStatus(Long playerProfileId, ClubVerificationRequestStatus status);
}
