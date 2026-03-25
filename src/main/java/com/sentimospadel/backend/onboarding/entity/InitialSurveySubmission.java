package com.sentimospadel.backend.onboarding.entity;

import com.sentimospadel.backend.onboarding.enums.AnswerOption;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import com.sentimospadel.backend.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "initial_survey_submissions")
public class InitialSurveySubmission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile playerProfile;

    @Column(name = "survey_version", nullable = false)
    private Integer surveyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "q1", nullable = false, length = 1, columnDefinition = "varchar(1)")
    private AnswerOption q1;

    @Enumerated(EnumType.STRING)
    @Column(name = "q2", nullable = false, length = 1, columnDefinition = "varchar(1)")
    private AnswerOption q2;

    @Enumerated(EnumType.STRING)
    @Column(name = "q3", nullable = false, length = 1, columnDefinition = "varchar(1)")
    private AnswerOption q3;

    @Enumerated(EnumType.STRING)
    @Column(name = "q4", nullable = false, length = 1, columnDefinition = "varchar(1)")
    private AnswerOption q4;

    @Enumerated(EnumType.STRING)
    @Column(name = "q5", nullable = false, length = 1, columnDefinition = "varchar(1)")
    private AnswerOption q5;

    @Enumerated(EnumType.STRING)
    @Column(name = "q6", nullable = false, length = 1, columnDefinition = "varchar(1)")
    private AnswerOption q6;

    @Enumerated(EnumType.STRING)
    @Column(name = "q7", nullable = false, length = 1, columnDefinition = "varchar(1)")
    private AnswerOption q7;

    @Enumerated(EnumType.STRING)
    @Column(name = "q8", nullable = false, length = 1, columnDefinition = "varchar(1)")
    private AnswerOption q8;

    @Enumerated(EnumType.STRING)
    @Column(name = "q9", nullable = false, length = 1, columnDefinition = "varchar(1)")
    private AnswerOption q9;

    @Enumerated(EnumType.STRING)
    @Column(name = "q10", nullable = false, length = 1, columnDefinition = "varchar(1)")
    private AnswerOption q10;

    @Column(name = "weighted_score", nullable = false)
    private Integer weightedScore;

    @Column(name = "normalized_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal normalizedScore;

    @Column(name = "initial_rating", nullable = false, precision = 4, scale = 2)
    private BigDecimal initialRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "estimated_category", nullable = false, length = 20)
    private UruguayCategory estimatedCategory;

    @Column(name = "requires_club_verification", nullable = false)
    private boolean requiresClubVerification;
}
