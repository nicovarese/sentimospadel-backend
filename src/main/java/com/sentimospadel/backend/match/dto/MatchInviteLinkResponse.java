package com.sentimospadel.backend.match.dto;

import java.time.Instant;

public record MatchInviteLinkResponse(
        Long matchId,
        String inviteToken,
        String inviteUrl,
        Instant expiresAt
) {
}
