package com.eadalat.notify.controller;

import com.eadalat.notify.entity.Notification;
import com.eadalat.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    /** The JWT filter stores the token subject (user id) as the principal. */
    private static Long currentUserId(Authentication auth) {
        return Long.valueOf(String.valueOf(auth.getPrincipal()));
    }

    @GetMapping
    public ResponseEntity<List<Notification>> list(
            Authentication auth,
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly) {
        return ResponseEntity.ok(service.list(currentUserId(auth), unreadOnly));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markRead(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(service.markRead(currentUserId(auth), id));
    }
}
