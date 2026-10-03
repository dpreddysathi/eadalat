package com.eadalat.notify.service;

import com.eadalat.notify.entity.Notification;
import com.eadalat.notify.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository repository;

    /**
     * Persist a notification and simulate its delivery.
     * Never logs or receives tokens here — callers pass plain fields only.
     */
    @Transactional
    public Notification notify(Long userId, Long caseId, String type, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .caseId(caseId)
                .type(type)
                .message(message)
                .read(false)
                .build();
        Notification saved = repository.save(notification);
        log.info("NOTIFY user={} : {}", userId, message);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Notification> list(Long userId, boolean unreadOnly) {
        return unreadOnly
                ? repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                : repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Notification markRead(Long userId, Long notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        notification.setRead(true);
        return repository.save(notification);
    }
}
