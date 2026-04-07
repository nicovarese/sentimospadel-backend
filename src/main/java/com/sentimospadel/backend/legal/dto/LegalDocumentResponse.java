package com.sentimospadel.backend.legal.dto;

import com.sentimospadel.backend.legal.enums.LegalDocumentType;

public record LegalDocumentResponse(
        LegalDocumentType type,
        String title,
        String version,
        boolean required,
        String content
) {
}
