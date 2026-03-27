package com.sentimospadel.backend.coach.service;

import com.sentimospadel.backend.coach.dto.CoachResponse;
import com.sentimospadel.backend.coach.entity.Coach;
import com.sentimospadel.backend.coach.repository.CoachRepository;
import com.sentimospadel.backend.player.support.UruguayCategoryMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoachService {

    private final CoachRepository coachRepository;

    @Transactional(readOnly = true)
    public List<CoachResponse> getCoaches() {
        return coachRepository.findAllByActiveTrueOrderByAverageRatingDescFullNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CoachResponse toResponse(Coach coach) {
        return new CoachResponse(
                coach.getId(),
                coach.getFullName(),
                coach.getClubName(),
                coach.getCurrentRating(),
                UruguayCategoryMapper.fromRating(coach.getCurrentRating()),
                coach.getReviewsCount(),
                coach.getAverageRating(),
                coach.getHourlyRateUyu(),
                coach.getPhone(),
                coach.getPhotoUrl(),
                coach.getCreatedAt(),
                coach.getUpdatedAt()
        );
    }
}
