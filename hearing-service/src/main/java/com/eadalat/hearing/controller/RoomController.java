package com.eadalat.hearing.controller;

import com.eadalat.hearing.service.RoomTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomTracker roomTracker;

    @GetMapping("/{roomId}/participants")
    public Map<String, Object> participants(@PathVariable String roomId) {
        Set<String> users = new TreeSet<>(roomTracker.participants(roomId));
        return Map.of(
                "roomId", roomId,
                "count", users.size(),
                "users", users
        );
    }
}
