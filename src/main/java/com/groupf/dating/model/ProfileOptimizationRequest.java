package com.groupf.dating.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupf.dating.util.StringListConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "profile_requests")
public class ProfileOptimizationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "original_bio", columnDefinition = "TEXT")
    private String originalBio;

    @Column(name = "tone_preference")
    private String tonePreference;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Convert(converter = StringListConverter.class)
    @Column(name = "rewritten_bios", columnDefinition = "TEXT")
    private List<String> rewrittenBios;

    @Convert(converter = StringListConverter.class)
    @Column(name = "conversation_starters", columnDefinition = "TEXT")
    private List<String> conversationStarters;

    @Column(name = "ranked_photos", columnDefinition = "TEXT")
    private String rankedPhotosJson;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhotoRanking {
        private String photoUrl;
        private int rank;
        private double score;
        private String reasoning;
    }

    public List<PhotoRanking> getRankedPhotos() {
        if (rankedPhotosJson == null || rankedPhotosJson.isBlank()) return new ArrayList<>();
        try {
            return new ObjectMapper().readValue(rankedPhotosJson, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void setRankedPhotos(List<PhotoRanking> photos) {
        try {
            this.rankedPhotosJson = new ObjectMapper().writeValueAsString(photos);
        } catch (Exception e) {
            this.rankedPhotosJson = "[]";
        }
    }
}
