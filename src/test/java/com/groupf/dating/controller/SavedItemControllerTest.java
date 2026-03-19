package com.groupf.dating.controller;

import com.groupf.dating.dto.HistoryResponse;
import com.groupf.dating.dto.SaveContentRequest;
import com.groupf.dating.dto.SavedItemResponse;
import com.groupf.dating.service.SavedItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedItemControllerTest {

    @Mock
    private SavedItemService savedItemService;

    @InjectMocks
    private SavedItemController controller;

    private JwtAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "ES256")
                .subject("user-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        auth = new JwtAuthenticationToken(jwt);
    }

    // ──────────── GET /api/profile/history ────────────

    @Test
    void getHistory_returnsHistoryForAuthenticatedUser() {
        HistoryResponse expected = new HistoryResponse(
                List.of(savedItemResponse("Bio content")),
                List.of(savedItemResponse("Starter content"))
        );
        when(savedItemService.getHistory("user-123")).thenReturn(expected);

        ResponseEntity<HistoryResponse> response = controller.getHistory(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSavedBios()).hasSize(1);
        assertThat(response.getBody().getSavedStarters()).hasSize(1);
    }

    @Test
    void getHistory_usesUserIdFromJwtSubject() {
        when(savedItemService.getHistory("user-123")).thenReturn(new HistoryResponse(List.of(), List.of()));

        controller.getHistory(auth);

        verify(savedItemService).getHistory("user-123");
        verify(savedItemService, never()).getHistory(argThat(id -> !id.equals("user-123")));
    }

    // ──────────── POST /api/profile/history/bio ────────────

    @Test
    void saveBio_persistsWithCorrectUserIdAndContent() {
        SaveContentRequest req = new SaveContentRequest();
        req.setContent("My awesome bio");

        SavedItemResponse saved = savedItemResponse("My awesome bio");
        when(savedItemService.saveBio("user-123", "My awesome bio")).thenReturn(saved);

        ResponseEntity<SavedItemResponse> response = controller.saveBio(req, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).isEqualTo("My awesome bio");
        verify(savedItemService).saveBio("user-123", "My awesome bio");
    }

    // ──────────── DELETE /api/profile/history/bio/{id} ────────────

    @Test
    void deleteBio_existingItem_returns204() {
        UUID id = UUID.randomUUID();
        doNothing().when(savedItemService).deleteItem(id, "user-123");

        ResponseEntity<Void> response = controller.deleteBio(id, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(savedItemService).deleteItem(id, "user-123");
    }

    @Test
    void deleteBio_itemNotFound_propagates404() {
        UUID id = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(savedItemService).deleteItem(eq(id), any());

        assertThatThrownBy(() -> controller.deleteBio(id, auth))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteBio_cannotDeleteAnotherUsersItem() {
        UUID id = UUID.randomUUID();
        // Service enforces user isolation — throws 404 when user-123 tries to delete user-456's item
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(savedItemService).deleteItem(id, "user-123");

        assertThatThrownBy(() -> controller.deleteBio(id, auth))
                .isInstanceOf(ResponseStatusException.class);
        verify(savedItemService).deleteItem(id, "user-123");
    }

    // ──────────── POST /api/profile/history/starter ────────────

    @Test
    void saveStarter_persistsWithCorrectUserIdAndContent() {
        SaveContentRequest req = new SaveContentRequest();
        req.setContent("Great opener!");

        SavedItemResponse saved = savedItemResponse("Great opener!");
        when(savedItemService.saveStarter("user-123", "Great opener!")).thenReturn(saved);

        ResponseEntity<SavedItemResponse> response = controller.saveStarter(req, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(savedItemService).saveStarter("user-123", "Great opener!");
    }

    // ──────────── DELETE /api/profile/history/starter/{id} ────────────

    @Test
    void deleteStarter_existingItem_returns204() {
        UUID id = UUID.randomUUID();
        doNothing().when(savedItemService).deleteItem(id, "user-123");

        ResponseEntity<Void> response = controller.deleteStarter(id, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ──────────── helpers ────────────

    private SavedItemResponse savedItemResponse(String content) {
        SavedItemResponse r = new SavedItemResponse();
        r.setId(UUID.randomUUID());
        r.setContent(content);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }
}
