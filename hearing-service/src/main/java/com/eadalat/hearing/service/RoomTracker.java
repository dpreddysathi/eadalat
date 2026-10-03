package com.eadalat.hearing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomTracker {

    private final ConcurrentHashMap<String, Set<String>> rooms = new ConcurrentHashMap<>();

    /** Adds userId to the room; returns the roomId for convenience. */
    public void join(String roomId, String userId) {
        if (roomId == null || userId == null) {
            return;
        }
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    /** Removes userId from the room; drops empty rooms. */
    public void leave(String roomId, String userId) {
        if (roomId == null || userId == null) {
            return;
        }
        rooms.computeIfPresent(roomId, (k, set) -> {
            set.remove(userId);
            return set.isEmpty() ? null : set;
        });
    }

    public Set<String> participants(String roomId) {
        return Collections.unmodifiableSet(rooms.getOrDefault(roomId, Set.of()));
    }

    /**
     * Removes userId from every room it belongs to. Returns the roomIds it was
     * removed from so callers can broadcast LEAVE notifications.
     */
    public Set<String> removeUserFromAllRooms(String userId) {
        if (userId == null) {
            return Set.of();
        }
        return rooms.entrySet().stream()
                .filter(e -> e.getValue().contains(userId))
                .map(Map.Entry::getKey)
                .peek(roomId -> leave(roomId, userId))
                .collect(Collectors.toSet());
    }
}
