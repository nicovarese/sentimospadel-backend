package com.sentimospadel.backend.match.service;

import com.sentimospadel.backend.config.security.JwtProperties;
import com.sentimospadel.backend.match.config.MatchInvitationProperties;
import com.sentimospadel.backend.match.dto.MatchInviteLinkResponse;
import com.sentimospadel.backend.match.dto.MatchInvitePreviewResponse;
import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
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
public class MatchInviteService {

    private static final String TOKEN_SUBJECT = "match-invite";
    private static final String CLAIM_MATCH_ID = "matchId";

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final PlayerProfileResolverService playerProfileResolverService;
    private final JwtProperties jwtProperties;
    private final MatchInvitationProperties matchInvitationProperties;

    @Transactional(readOnly = true)
    public MatchInviteLinkResponse createInviteLink(String email, Long matchId) {
        Match match = requireMatch(matchId);
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);

        if (!matchParticipantRepository.existsByMatchIdAndPlayerProfileId(matchId, playerProfile.getId())) {
            throw new BadRequestException("Solo los jugadores del partido pueden generar links de invitacion.");
        }

        if (match.getStatus() == MatchStatus.PENDING_CLUB_CONFIRMATION) {
            throw new BadRequestException("La reserva todavia esta pendiente de aprobacion del club.");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(matchInvitationProperties.expiration());
        String token = buildToken(matchId, now, expiresAt);
        String inviteUrl = UriComponentsBuilder.fromUriString(normalizedBaseUrl())
                .replaceQuery(null)
                .queryParam("matchInvite", token)
                .build()
                .toUriString();

        return new MatchInviteLinkResponse(match.getId(), token, inviteUrl, expiresAt);
    }

    @Transactional(readOnly = true)
    public MatchInvitePreviewResponse resolveInvite(String token) {
        Claims claims = parseInviteToken(token);
        Long matchId = claims.get(CLAIM_MATCH_ID, Long.class);
        if (matchId == null) {
            throw new BadRequestException("El link de invitacion no es valido.");
        }

        Match match = requireMatch(matchId);
        long participantCount = matchParticipantRepository.countByMatchId(matchId);

        return new MatchInvitePreviewResponse(
                match.getId(),
                match.getStatus(),
                match.getScheduledAt(),
                match.getClub() == null ? null : match.getClub().getId(),
                match.getClub() == null ? null : match.getClub().getName(),
                resolveCourtName(match),
                match.getLocationText(),
                match.getCreatedBy().getFullName(),
                (int) participantCount,
                match.getMaxPlayers(),
                claims.getExpiration().toInstant()
        );
    }

    private String buildToken(Long matchId, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .subject(TOKEN_SUBJECT)
                .claim(CLAIM_MATCH_ID, matchId)
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
                throw new BadRequestException("El link de invitacion no es valido.");
            }

            return claims;
        } catch (JwtException exception) {
            throw new BadRequestException("El link de invitacion no es valido o ya vencio.");
        }
    }

    private Match requireMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match with id " + matchId + " was not found"));
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private String normalizedBaseUrl() {
        String baseUrl = matchInvitationProperties.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Match invitation base URL is not configured");
        }
        return baseUrl.trim();
    }

    private String requireToken(String token) {
        if (token == null || token.trim().isBlank()) {
            throw new BadRequestException("El token de invitacion es obligatorio.");
        }
        return token.trim();
    }

    private String resolveCourtName(Match match) {
        String locationText = match.getLocationText();
        if (locationText == null || locationText.isBlank()) {
            return "Cancha por definir";
        }

        String[] parts = locationText.split("\\s*(?:-|·|•)\\s*", 2);
        if (parts.length == 2 && !parts[1].isBlank()) {
            return parts[1].trim();
        }

        return locationText.trim();
    }
}
