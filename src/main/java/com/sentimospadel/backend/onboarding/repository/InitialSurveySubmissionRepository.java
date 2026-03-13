package com.sentimospadel.backend.onboarding.repository;

import com.sentimospadel.backend.onboarding.entity.InitialSurveySubmission;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InitialSurveySubmissionRepository extends JpaRepository<InitialSurveySubmission, Long> {

    @EntityGraph(attributePaths = {"playerProfile", "playerProfile.user"})
    Optional<InitialSurveySubmission> findTopByPlayerProfileIdOrderByCreatedAtDesc(Long playerProfileId);
}
