package com.eadalat.hearing.controller;

import com.eadalat.hearing.dto.SignalMessage;
import com.eadalat.hearing.dto.SignalType;
import com.eadalat.hearing.service.RoomTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SignalController {

    private final RoomTracker roomTracker;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/signal/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public SignalMessage signal(@DestinationVariable String roomId, SignalMessage msg) {
        if (msg == null) {
            return null;
        }
        String from = msg.getFrom();
        SignalType type = msg.getType();
        if (type == SignalType.JOIN) {
            roomTracker.join(roomId, from);
            log.info("User {} joined hearing room {}", from, roomId);
        } else if (type == SignalType.LEAVE) {
            roomTracker.leave(roomId, from);
            log.info("User {} left hearing room {}", from, roomId);
        }
        // OFFER / ANSWER / ICE are relayed untouched (echo to the room topic).
        return msg;
    }

    /**
     * Best-effort cleanup on WebSocket disconnect: remove the session's user
     * from every room they were tracked in and broadcast a LEAVE to each room
     * topic so peers update their participant lists. Everything is null-safe;
     * a client that never sent JOIN simply has no tracked user.
     */
    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        try {
            Map<String, Object> sessionAttrs =
                    SimpMessageHeaderAccessor.getSessionAttributes(event.getMessage().getHeaders());
            String userId = sessionAttrs != null ? (String) sessionAttrs.get("userId") : null;
            if (userId == null) {
                // Nothing tracked for this session — nothing to clean up.
                return;
            }
            Set<String> rooms = roomTracker.removeUserFromAllRooms(userId);
            for (String roomId : rooms) {
                SignalMessage leave = new SignalMessage(userId, SignalType.LEAVE, null, null);
                messagingTemplate.convertAndSend("/topic/room/" + roomId, leave);
            }
            log.info("Session disconnect: removed user {} from {} room(s)", userId, rooms.size());
        } catch (Exception e) {
            log.warn("Best-effort disconnect cleanup failed", e);
        }
    }
}
