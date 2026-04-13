package com.sentimospadel.backend.player.service;

import com.sentimospadel.backend.player.config.PlayerProfilePhotoStorageProperties;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PlayerProfilePhotoStorageService {

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png",
            "image/webp", ".webp"
    );

    private final PlayerProfilePhotoStorageProperties properties;

    @PostConstruct
    void ensureStorageDirectoryExists() {
        createStorageDirectoryIfNeeded();
    }

    public StoredPlayerProfilePhoto store(Long playerProfileId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Debes seleccionar una imagen.");
        }

        if (file.getSize() > properties.maxSizeBytes()) {
            throw new BadRequestException("La foto no puede superar los 5 MB.");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!CONTENT_TYPE_TO_EXTENSION.containsKey(contentType)) {
            throw new BadRequestException("La foto debe estar en JPG, PNG o WEBP.");
        }

        String filename = buildFilename(playerProfileId, contentType);
        Path targetPath = resolveStoragePath(filename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo guardar la foto del perfil.", exception);
        }

        return new StoredPlayerProfilePhoto(filename, buildPublicUrl(filename));
    }

    public StoredPlayerProfilePhotoResource load(String filename) {
        String normalizedFilename = normalizeFilename(filename);
        Path filePath = resolveStoragePath(normalizedFilename);

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new ResourceNotFoundException("Player profile photo was not found");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            MediaType mediaType = MediaTypeFactory.getMediaType(normalizedFilename)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return new StoredPlayerProfilePhotoResource(resource, mediaType);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer la foto del perfil.", exception);
        }
    }

    public void deleteManagedPhotoIfPresent(String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) {
            return;
        }

        String normalizedBaseUrl = normalizedPublicBaseUrl();
        String normalizedPhotoUrl = photoUrl.trim();
        if (!normalizedPhotoUrl.startsWith(normalizedBaseUrl + "/")) {
            return;
        }

        String filename = normalizedPhotoUrl.substring(normalizedBaseUrl.length() + 1);
        Path filePath = resolveStoragePath(filename);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo borrar la foto anterior del perfil.", exception);
        }
    }

    private String buildFilename(Long playerProfileId, String contentType) {
        return "player-" + playerProfileId + "-"
                + Instant.now().toEpochMilli() + "-"
                + UUID.randomUUID().toString().replace("-", "")
                + CONTENT_TYPE_TO_EXTENSION.get(contentType);
    }

    private String buildPublicUrl(String filename) {
        return normalizedPublicBaseUrl() + "/" + filename;
    }

    private String normalizedPublicBaseUrl() {
        return properties.publicBaseUrl().replaceAll("/+$", "");
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private Path resolveStoragePath(String filename) {
        Path storageDirectory = createStorageDirectoryIfNeeded();
        Path resolved = storageDirectory.resolve(normalizeFilename(filename)).normalize();
        if (!resolved.startsWith(storageDirectory)) {
            throw new BadRequestException("Nombre de archivo invalido.");
        }
        return resolved;
    }

    private String normalizeFilename(String filename) {
        String trimmed = filename == null ? "" : filename.trim();
        if (trimmed.isBlank() || trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("..")) {
            throw new ResourceNotFoundException("Player profile photo was not found");
        }
        return trimmed;
    }

    private Path createStorageDirectoryIfNeeded() {
        Path storageDirectory = Paths.get(properties.storagePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageDirectory);
            return storageDirectory;
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo preparar el storage de fotos de perfil.", exception);
        }
    }
}
