package com.sentimospadel.backend.rating.service;

import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.player.support.UruguayCategoryMapper;
import com.sentimospadel.backend.rating.dto.RankingEntryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final PlayerProfileRepository playerProfileRepository;

    @Transactional(readOnly = true)
    public List<RankingEntryResponse> getRankings() {
        List<com.sentimospadel.backend.player.entity.PlayerProfile> profiles = playerProfileRepository.findAllByOrderByCurrentRatingDescFullNameAsc();

        return java.util.stream.IntStream.range(0, profiles.size())
                .mapToObj(index -> {
                    com.sentimospadel.backend.player.entity.PlayerProfile profile = profiles.get(index);
                    return new RankingEntryResponse(
                            index + 1,
                            profile.getId(),
                            profile.getFullName(),
                            profile.getCity(),
                            profile.getCurrentRating(),
                            UruguayCategoryMapper.fromRating(profile.getCurrentRating()),
                            profile.getRatedMatchesCount()
                    );
                })
                .toList();
    }
}
