package com.groupf.dating.controller;

import com.groupf.dating.dto.HistoryResponse;
import com.groupf.dating.dto.SaveContentRequest;
import com.groupf.dating.dto.SavedItemResponse;
import com.groupf.dating.service.SavedItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile/history")
@RequiredArgsConstructor
public class SavedItemController {

    private final SavedItemService savedItemService;

    @GetMapping
    public ResponseEntity<HistoryResponse> getHistory(JwtAuthenticationToken auth) {
        String userId = auth.getToken().getSubject();
        return ResponseEntity.ok(savedItemService.getHistory(userId));
    }

    @PostMapping("/bio")
    public ResponseEntity<SavedItemResponse> saveBio(
            @Valid @RequestBody SaveContentRequest req,
            JwtAuthenticationToken auth) {
        String userId = auth.getToken().getSubject();
        return ResponseEntity.ok(savedItemService.saveBio(userId, req.getContent()));
    }

    @DeleteMapping("/bio/{id}")
    public ResponseEntity<Void> deleteBio(@PathVariable UUID id, JwtAuthenticationToken auth) {
        String userId = auth.getToken().getSubject();
        savedItemService.deleteItem(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/starter")
    public ResponseEntity<SavedItemResponse> saveStarter(
            @Valid @RequestBody SaveContentRequest req,
            JwtAuthenticationToken auth) {
        String userId = auth.getToken().getSubject();
        return ResponseEntity.ok(savedItemService.saveStarter(userId, req.getContent()));
    }

    @DeleteMapping("/starter/{id}")
    public ResponseEntity<Void> deleteStarter(@PathVariable UUID id, JwtAuthenticationToken auth) {
        String userId = auth.getToken().getSubject();
        savedItemService.deleteItem(id, userId);
        return ResponseEntity.noContent().build();
    }
}
