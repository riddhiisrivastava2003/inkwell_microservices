package com.inkwell.media_service.service;

import com.inkwell.media_service.exception.BadRequestException;
import com.inkwell.media_service.model.Media;
import com.inkwell.media_service.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final WebClient webClient;

    @Value("${media.storage-path}")
    private String storagePath;

    @Value("${media.public-base-url}")
    private String publicBaseUrl;
    @Value("${services.auth.base-url}")
    private String authBaseUrl;

    public Media upload(Long uploaderId, MultipartFile file, String altText, Long linkedPostId) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        Path folder = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(folder);
            String ext = getExtension(Objects.requireNonNull(file.getOriginalFilename()));
            String filename = UUID.randomUUID() + (ext.isBlank() ? "" : "." + ext);
            Path target = folder.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            Media media = new Media();
            media.setUploaderId(uploaderId);
            media.setFilename(filename);
            media.setOriginalName(file.getOriginalFilename());
            media.setMimeType(file.getContentType());
            media.setSizeKb(Math.max(1L, file.getSize() / 1024));
            media.setAltText(altText);
            media.setLinkedPostId(linkedPostId);
            media.setUrl(publicBaseUrl + "/files/" + filename);
            return mediaRepository.save(media);
        } catch (IOException e) {
            throw new BadRequestException("Unable to save file");
        }
    }

    public List<Media> getAll() {
        return mediaRepository.findByDeletedFalse();
    }

    public List<Media> getByUploader(Long uploaderId) {
        return mediaRepository.findByUploaderIdAndDeletedFalse(uploaderId);
    }

    public List<Media> getByPost(Long postId) {
        return mediaRepository.findByLinkedPostIdAndDeletedFalse(postId);
    }

    public Media updateAltText(Long mediaId, String altText) {
        Media media = getById(mediaId);
        media.setAltText(altText);
        return mediaRepository.save(media);
    }

    public Media linkToPost(Long mediaId, Long postId) {
        Media media = getById(mediaId);
        media.setLinkedPostId(postId);
        return mediaRepository.save(media);
    }

    public Media unlinkFromPost(Long mediaId) {
        Media media = getById(mediaId);
        media.setLinkedPostId(null);
        return mediaRepository.save(media);
    }

    public void delete(Long mediaId, Long actorUserId) {
        Media media = getById(mediaId);
        media.setDeleted(true);
        mediaRepository.save(media);
        logAudit(actorUserId, "DELETE_MEDIA", "MEDIA", mediaId, "media deleted");
    }

    public Resource loadFile(String filename) {
        try {
            Path path = Paths.get(storagePath).toAbsolutePath().normalize().resolve(filename).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                throw new BadRequestException("File not found");
            }
            return resource;
        } catch (Exception e) {
            throw new BadRequestException("File not found");
        }
    }

    private Media getById(Long mediaId) {
        return mediaRepository.findById(mediaId).orElseThrow(() -> new BadRequestException("Media not found"));
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx < 0 ? "" : filename.substring(idx + 1);
    }

    private void logAudit(Long actorUserId, String action, String entityType, Long entityId, String details) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("actorUserId", actorUserId);
            payload.put("action", action);
            payload.put("entityType", entityType);
            payload.put("entityId", String.valueOf(entityId));
            payload.put("details", details);
            webClient.post().uri(authBaseUrl + "/api/auth/internal/audit-log").bodyValue(payload).retrieve().toBodilessEntity().block();
        } catch (Exception ignored) {
            // best effort
        }
    }
}


