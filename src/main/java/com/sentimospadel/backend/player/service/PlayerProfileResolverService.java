package com.sentimospadel.backend.player.service;

import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerProfileResolverService {

    private final UserRepository userRepository;
    private final PlayerProfileRepository playerProfileRepository;

    @Transactional
    public PlayerProfile getOrCreateByUserEmail(String email) {
        User user = getUserByEmail(email);

        return playerProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> playerProfileRepository.save(PlayerProfile.builder()
                        .user(user)
                        .fullName(deriveFullName(user.getEmail()))
                        .currentRating(BigDecimal.valueOf(1.00).setScale(2))
                        .provisional(true)
                        .matchesPlayed(0)
                        .ratedMatchesCount(0)
                        .surveyCompleted(false)
                        .requiresClubVerification(false)
                        .clubVerificationStatus(ClubVerificationStatus.NOT_REQUIRED)
                        .build()));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String deriveFullName(String email) {
        String localPart = email == null ? "Player" : email.split("@", 2)[0];
        String normalized = localPart.replace('.', ' ').replace('_', ' ').replace('-', ' ').trim();

        if (normalized.isBlank()) {
            return "Player";
        }

        String[] words = normalized.split("\\s+");
        StringBuilder fullName = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                fullName.append(' ');
            }
            String word = words[i];
            fullName.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                fullName.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return fullName.toString();
    }
}
