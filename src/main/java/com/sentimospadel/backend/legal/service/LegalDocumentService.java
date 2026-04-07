package com.sentimospadel.backend.legal.service;

import com.sentimospadel.backend.legal.dto.LegalDocumentResponse;
import com.sentimospadel.backend.legal.enums.LegalDocumentType;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class LegalDocumentService {

    private static final Map<LegalDocumentType, DocumentDefinition> DOCUMENTS = new EnumMap<>(LegalDocumentType.class);

    static {
        DOCUMENTS.put(
                LegalDocumentType.TERMS_AND_CONDITIONS,
                new DocumentDefinition(
                        "Términos y Condiciones",
                        "2026-04-07.1",
                        true,
                        "legal/terms-and-conditions.md"
                )
        );
        DOCUMENTS.put(
                LegalDocumentType.PRIVACY_POLICY,
                new DocumentDefinition(
                        "Política de Privacidad y Tratamiento de Datos",
                        "2026-04-07.1",
                        true,
                        "legal/privacy-policy.md"
                )
        );
        DOCUMENTS.put(
                LegalDocumentType.CONSENT_PREFERENCES_NOTICE,
                new DocumentDefinition(
                        "Consentimientos de Actividad y Notificaciones",
                        "2026-04-07.1",
                        false,
                        "legal/consent-preferences-notice.md"
                )
        );
    }

    public List<LegalDocumentResponse> getDocuments() {
        return DOCUMENTS.entrySet().stream()
                .map(entry -> toResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    public String getCurrentTermsVersion() {
        return definitionFor(LegalDocumentType.TERMS_AND_CONDITIONS).version();
    }

    public String getCurrentPrivacyVersion() {
        return definitionFor(LegalDocumentType.PRIVACY_POLICY).version();
    }

    public String getCurrentConsentPreferencesVersion() {
        return definitionFor(LegalDocumentType.CONSENT_PREFERENCES_NOTICE).version();
    }

    public void validateTermsVersion(String acceptedVersion) {
        validateVersion(
                LegalDocumentType.TERMS_AND_CONDITIONS,
                acceptedVersion,
                "Debes aceptar la versión vigente de los Términos y Condiciones."
        );
    }

    public void validatePrivacyVersion(String acceptedVersion) {
        validateVersion(
                LegalDocumentType.PRIVACY_POLICY,
                acceptedVersion,
                "Debes aceptar la versión vigente de la Política de Privacidad."
        );
    }

    public void validateConsentPreferencesVersion(String acceptedVersion) {
        validateVersion(
                LegalDocumentType.CONSENT_PREFERENCES_NOTICE,
                acceptedVersion,
                "Debes revisar la versión vigente de los consentimientos de actividad y notificaciones."
        );
    }

    private void validateVersion(LegalDocumentType type, String acceptedVersion, String message) {
        String expectedVersion = definitionFor(type).version();
        if (acceptedVersion == null || acceptedVersion.isBlank() || !expectedVersion.equals(acceptedVersion.trim())) {
            throw new BadRequestException(message);
        }
    }

    private LegalDocumentResponse toResponse(LegalDocumentType type, DocumentDefinition definition) {
        return new LegalDocumentResponse(
                type,
                definition.title(),
                definition.version(),
                definition.required(),
                readContent(definition.resourcePath())
        );
    }

    private DocumentDefinition definitionFor(LegalDocumentType type) {
        return DOCUMENTS.get(type);
    }

    private String readContent(String resourcePath) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo cargar el documento legal " + resourcePath, exception);
        }
    }

    private record DocumentDefinition(
            String title,
            String version,
            boolean required,
            String resourcePath
    ) {
    }
}
