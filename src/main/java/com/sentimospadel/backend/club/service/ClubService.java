package com.sentimospadel.backend.club.service;

import com.sentimospadel.backend.club.dto.ClubResponse;
import com.sentimospadel.backend.club.dto.CreateClubRequest;
import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.repository.ClubRepository;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;

    @Transactional
    public ClubResponse createClub(CreateClubRequest request) {
        Club club = Club.builder()
                .name(request.name().trim())
                .city(request.city().trim())
                .address(trimToNull(request.address()))
                .description(trimToNull(request.description()))
                .integrated(request.integrated())
                .build();

        return toResponse(clubRepository.save(club));
    }

    @Transactional(readOnly = true)
    public ClubResponse getClubById(Long id) {
        Club club = clubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Club with id " + id + " was not found"));

        return toResponse(club);
    }

    @Transactional(readOnly = true)
    public List<ClubResponse> getClubs() {
        return clubRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ClubResponse toResponse(Club club) {
        return new ClubResponse(
                club.getId(),
                club.getName(),
                club.getCity(),
                club.getAddress(),
                club.getDescription(),
                club.isIntegrated(),
                club.getCreatedAt(),
                club.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
