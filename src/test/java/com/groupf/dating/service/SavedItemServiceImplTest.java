package com.groupf.dating.service;

import com.groupf.dating.dto.HistoryResponse;
import com.groupf.dating.dto.SavedItemResponse;
import com.groupf.dating.model.SavedItem;
import com.groupf.dating.repository.SavedItemRepository;
import com.groupf.dating.service.impl.SavedItemServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedItemServiceImplTest {

    @Mock
    private SavedItemRepository savedItemRepository;

    @InjectMocks
    private SavedItemServiceImpl savedItemService;

    // ──────────────────────── saveBio ────────────────────────

    @Test
    void saveBio_persistsItemWithCorrectTypeAndUserId() {
        SavedItem saved = savedItem(UUID.randomUUID(), "user-1", SavedItem.ItemType.BIO, "My awesome bio");
        when(savedItemRepository.save(any())).thenReturn(saved);

        SavedItemResponse response = savedItemService.saveBio("user-1", "My awesome bio");

        assertThat(response.getContent()).isEqualTo("My awesome bio");
        verify(savedItemRepository).save(argThat(item ->
                "user-1".equals(item.getUserId()) &&
                SavedItem.ItemType.BIO == item.getType() &&
                "My awesome bio".equals(item.getContent())
        ));
    }

    // ──────────────────────── saveStarter ────────────────────

    @Test
    void saveStarter_persistsItemWithCorrectType() {
        SavedItem saved = savedItem(UUID.randomUUID(), "user-1", SavedItem.ItemType.STARTER, "Hey, love your hiking photos!");
        when(savedItemRepository.save(any())).thenReturn(saved);

        SavedItemResponse response = savedItemService.saveStarter("user-1", "Hey, love your hiking photos!");

        assertThat(response.getContent()).isEqualTo("Hey, love your hiking photos!");
        verify(savedItemRepository).save(argThat(item ->
                SavedItem.ItemType.STARTER == item.getType()
        ));
    }

    // ──────────────────────── getHistory ─────────────────────

    @Test
    void getHistory_returnsOnlyItemsBelongingToRequestingUser() {
        UUID bioId = UUID.randomUUID();
        UUID starterId = UUID.randomUUID();

        when(savedItemRepository.findByUserIdAndTypeOrderByCreatedAtDesc("user-1", SavedItem.ItemType.BIO))
                .thenReturn(List.of(savedItem(bioId, "user-1", SavedItem.ItemType.BIO, "Bio content")));
        when(savedItemRepository.findByUserIdAndTypeOrderByCreatedAtDesc("user-1", SavedItem.ItemType.STARTER))
                .thenReturn(List.of(savedItem(starterId, "user-1", SavedItem.ItemType.STARTER, "Starter content")));

        HistoryResponse history = savedItemService.getHistory("user-1");

        assertThat(history.getSavedBios()).hasSize(1);
        assertThat(history.getSavedBios().get(0).getId()).isEqualTo(bioId);
        assertThat(history.getSavedStarters()).hasSize(1);
        assertThat(history.getSavedStarters().get(0).getId()).isEqualTo(starterId);
    }

    @Test
    void getHistory_returnsEmptyLists_whenUserHasNoSavedItems() {
        when(savedItemRepository.findByUserIdAndTypeOrderByCreatedAtDesc(anyString(), any()))
                .thenReturn(List.of());

        HistoryResponse history = savedItemService.getHistory("user-with-no-items");

        assertThat(history.getSavedBios()).isEmpty();
        assertThat(history.getSavedStarters()).isEmpty();
    }

    // ──────────────────────── deleteItem ─────────────────────

    @Test
    void deleteItem_deletesItem_whenItBelongsToUser() {
        UUID id = UUID.randomUUID();
        SavedItem item = savedItem(id, "user-1", SavedItem.ItemType.BIO, "Bio");
        when(savedItemRepository.findByIdAndUserId(id, "user-1")).thenReturn(Optional.of(item));

        savedItemService.deleteItem(id, "user-1");

        verify(savedItemRepository).delete(item);
    }

    @Test
    void deleteItem_throws404_whenItemDoesNotBelongToUser() {
        UUID id = UUID.randomUUID();
        // user-2 tries to delete user-1's item — repository returns empty
        when(savedItemRepository.findByIdAndUserId(id, "user-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savedItemService.deleteItem(id, "user-2"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void deleteItem_throws404_whenItemDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(savedItemRepository.findByIdAndUserId(eq(id), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savedItemService.deleteItem(id, "user-1"))
                .isInstanceOf(ResponseStatusException.class);
        verify(savedItemRepository, never()).delete(any());
    }

    // ──────────────────────── helpers ────────────────────────

    private SavedItem savedItem(UUID id, String userId, SavedItem.ItemType type, String content) {
        SavedItem item = new SavedItem();
        item.setId(id);
        item.setUserId(userId);
        item.setType(type);
        item.setContent(content);
        item.setCreatedAt(LocalDateTime.now());
        return item;
    }

    // ====== HANDWRITTEN TESTS ======

    @Test
    void saveBio_responseContainsCreatedAt() {
        // basic sanity check, the timestamp should come back in the response
        var id = UUID.randomUUID();
        SavedItem saved = savedItem(id, "user-42", SavedItem.ItemType.BIO, "Love surfing and sunsets");
        when(savedItemRepository.save(any())).thenReturn(saved);

        var response = savedItemService.saveBio("user-42", "Love surfing and sunsets");

        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void getHistory_whenUserHasMultipleBios_allAreReturned() {
        // make sure multiple items all come back, not just the first one
        var b1 = savedItem(UUID.randomUUID(), "user-5", SavedItem.ItemType.BIO, "Bio #1");
        var b2 = savedItem(UUID.randomUUID(), "user-5", SavedItem.ItemType.BIO, "Bio #2");
        var b3 = savedItem(UUID.randomUUID(), "user-5", SavedItem.ItemType.BIO, "Bio #3");

        when(savedItemRepository.findByUserIdAndTypeOrderByCreatedAtDesc("user-5", SavedItem.ItemType.BIO))
                .thenReturn(List.of(b1, b2, b3));
        when(savedItemRepository.findByUserIdAndTypeOrderByCreatedAtDesc("user-5", SavedItem.ItemType.STARTER))
                .thenReturn(List.of());

        var history = savedItemService.getHistory("user-5");

        assertThat(history.getSavedBios()).hasSize(3);
    }

    @Test
    void saveStarter_responseIdMatchesSavedItemId() {
        // make sure the id from the persisted entity is what gets returned
        var expectedId = UUID.randomUUID();
        SavedItem saved = savedItem(expectedId, "user-7", SavedItem.ItemType.STARTER, "Hey! I see you like hiking too");
        when(savedItemRepository.save(any())).thenReturn(saved);

        var response = savedItemService.saveStarter("user-7", "Hey! I see you like hiking too");

        assertThat(response.getId()).isEqualTo(expectedId);
    }
}
