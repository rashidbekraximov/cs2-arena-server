package uz.cs2arena.news;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record NewsDto(Long id, String category, String title, String body, boolean published, Instant createdAt) {

    public static NewsDto of(News n) {
        return new NewsDto(n.getId(), n.getCategory(), n.getTitle(), n.getBody(), n.isPublished(), n.getCreatedAt());
    }

    public record Request(@NotBlank @Size(max = 32) String category,
                          @NotBlank @Size(max = 160) String title,
                          @Size(max = 10000) String body,
                          boolean published) {}
}
