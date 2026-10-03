package com.eadalat.hearing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-hearing")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Remembers the JOINing user's userId in the STOMP session attributes so
     * the disconnect listener can attribute a best-effort LEAVE cleanup.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public org.springframework.messaging.Message<?> preSend(
                    org.springframework.messaging.Message<?> message,
                    org.springframework.messaging.MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (StompCommand.SEND.equals(accessor.getCommand())
                        && accessor.getDestination() != null
                        && accessor.getDestination().startsWith("/app/signal/")) {
                    String userId = firstHeader(message, "userId");
                    Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                    if (userId != null && sessionAttrs != null) {
                        sessionAttrs.put("userId", userId);
                    }
                }
                return message;
            }
        });
    }

    private static String firstHeader(org.springframework.messaging.Message<?> message, String name) {
        Object value = message.getHeaders().get(name);
        return value != null ? value.toString() : null;
    }
}
