package com.sentimospadel.backend.tournament.service;

import com.sentimospadel.backend.config.security.JwtProperties;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import com.sentimospadel.backend.tournament.config.TournamentInvitationProperties;
import com.sentimospadel.backend.tournament.dto.TournamentInviteLinkResponse;
import com.sentimospadel.backend.tournament.dto.TournamentInvitePreviewResponse;
import com.sentimospadel.backend.tournament.entity.Tournament;
import com.sentimospadel.backend.tournament.enums.TournamentStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TournamentInviteService {

    private static final String TOKEN_SUBJECT = "tournament-invite";
    private static final String CLAIM_TOURNAMENT_ID = "tournamentId";

    private final TournamentService tournamentService;
    private final PlayerProfileResolverService playerProfileResolverService;
    private final JwtProperties jwtProperties;
    private final TournamentInvitationProperties tournamentInvitationProperties;

    @Transactional(readOnly = true)
    public TournamentInviteLinkResponse createInviteLink(String email, Long tournamentId) {
        Tournament tournament = tournamentService.getTournamentEntity(tournamentId);
        PlayerProfile actor = playerProfileResolverService.getOrCreateByUserEmail(email);

        if (!tournament.getCreatedBy().getId().equals(actor.getId())) {
            throw new BadRequestException("Solo el creador del torneo puede generar links de invitacion.");
        }
        if (tournament.getStatus() != TournamentStatus.OPEN) {
            throw new BadRequestException("Solo se pueden compartir links mientras el torneo esta abierto.");
        }
        if (!tournament.isOpenEnrollment()) {
            throw new BadRequestException("Este torneo tiene inscripcion cerrada.");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(tournamentInvitationProperties.expiration());
        String token = buildToken(tournamentId, now, expiresAt);
        String inviteUrl = UriComponentsBuilder.fromUriString(normalizedBaseUrl())
                .replaceQuery(null)
                .queryParam("tournamentInvite", token)
                .build()
                .toUriString();

        return new TournamentInviteLinkResponse(tournament.getId(), token, inviteUrl, expiresAt);
    }

    @Transactional(readOnly = true)
    public TournamentInvitePreviewResponse resolveInvite(String token) {
        Claims claims = parseInviteToken(token);
        Long tournamentId = claims.get(CLAIM_TOURNAMENT_ID, Long.class);
        if (tournamentId == null) {
            throw new BadRequestException("El link del torneo no es valido.");
        }

        Tournament tournament = tournamentService.getTournamentEntity(tournamentId);
        TournamentResponseSnapshot snapshot = new TournamentResponseSnapshot(
                tournamentService.getTournamentById(tournamentId)
        );

        return new TournamentInvitePreviewResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getStatus(),
                tournament.getFormat(),
                tournament.isOpenEnrollment(),
                tournament.isCompetitive(),
                tournament.getCreatedBy().getFullName(),
                tournament.getClub() == null ? null : tournament.getClub().getId(),
                tournament.getClub() == null ? null : tournament.getClub().getName(),
                tournament.getCity(),
                tournament.getCategoryLabels() == null ? java.util.List.of() : java.util.List.copyOf(tournament.getCategoryLabels()),
                tournament.getStartDate(),
                tournament.getEndDate(),
                snapshot.currentEntriesCount(),
                snapshot.currentPlayersCount(),
                tournament.getMaxEntries(),
                claims.getExpiration().toInstant()
        );
    }

    private String buildToken(Long tournamentId, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .subject(TOKEN_SUBJECT)
                .claim(CLAIM_TOURNAMENT_ID, tournamentId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey())
                .compact();
    }

    private Claims parseInviteToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(requireToken(token))
                    .getPayload();

            if (!TOKEN_SUBJECT.equals(claims.getSubject())) {
                throw new BadRequestException("El link del torneo no es valido.");
            }

            return claims;
        } catch (JwtException exception) {
            throw new BadRequestException("El link del torneo no es valido o ya vencio.");
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private String normalizedBaseUrl() {
        String baseUrl = tournamentInvitationProperties.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Tournament invitation base URL is not configured");
        }
        return baseUrl.trim();
    }

    private String requireToken(String token) {
        if (token == null || token.trim().isBlank()) {
            throw new BadRequestException("El token de invitacion es obligatorio.");
        }
        return token.trim();
    }

    private record TournamentResponseSnapshot(
            int currentEntriesCount,
            int currentPlayersCount
    ) {
        private TournamentResponseSnapshot(com.sentimospadel.backend.tournament.dto.TournamentResponse response) {
            this(response.currentEntriesCount(), response.currentPlayersCount());
        }
    }
}
