package com.sentimospadel.backend.player.service;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record StoredPlayerProfilePhotoResource(
        Resource resource,
        MediaType mediaType
) {
}
