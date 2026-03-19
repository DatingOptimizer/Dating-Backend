package com.groupf.dating.repository;

import com.groupf.dating.model.SavedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedItemRepository extends JpaRepository<SavedItem, UUID> {
    List<SavedItem> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, SavedItem.ItemType type);
    Optional<SavedItem> findByIdAndUserId(UUID id, String userId);
}
