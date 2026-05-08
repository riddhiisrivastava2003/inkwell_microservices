package com.inkwell.media_service.controller;

import com.inkwell.media_service.model.Media;
import com.inkwell.media_service.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<Media> upload(@RequestParam(required = false) Long uploaderId,
                                        @RequestParam MultipartFile file,
                                        @RequestParam(required = false) String altText,
                                        @RequestParam(required = false) Long linkedPostId) {
        return ResponseEntity.ok(mediaService.upload(uploaderId, file, altText, linkedPostId));
    }

    @GetMapping
    public ResponseEntity<List<Media>> all() {
        return ResponseEntity.ok(mediaService.getAll());
    }

    @GetMapping("/uploader/{uploaderId}")
    public ResponseEntity<List<Media>> byUploader(@PathVariable Long uploaderId) {
        return ResponseEntity.ok(mediaService.getByUploader(uploaderId));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Media>> byPost(@PathVariable Long postId) {
        return ResponseEntity.ok(mediaService.getByPost(postId));
    }

    @PutMapping("/{mediaId}/alt")
    public ResponseEntity<Media> updateAlt(@PathVariable Long mediaId, @RequestParam String altText) {
        return ResponseEntity.ok(mediaService.updateAltText(mediaId, altText));
    }

    @PostMapping("/{mediaId}/link")
    public ResponseEntity<Media> link(@PathVariable Long mediaId, @RequestParam Long postId) {
        return ResponseEntity.ok(mediaService.linkToPost(mediaId, postId));
    }

    @PostMapping("/{mediaId}/unlink")
    public ResponseEntity<Media> unlink(@PathVariable Long mediaId) {
        return ResponseEntity.ok(mediaService.unlinkFromPost(mediaId));
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long mediaId,
                                                      @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        mediaService.delete(mediaId, userId);
        return ResponseEntity.ok(Map.of("message", "Media deleted"));
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource resource = mediaService.loadFile(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
