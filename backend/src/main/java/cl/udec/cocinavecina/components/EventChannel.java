package cl.udec.cocinavecina.components;

import cl.udec.cocinavecina.dto.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnWebApplication
public class EventChannel extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(EventChannel.class);
    private static final int SEND_TIMEOUT_MS = 5_000;
    private static final int BUFFER_BYTES = 64 * 1024;

    private final Map<Long, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        Long userId = userId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        sessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
            .put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, SEND_TIMEOUT_MS, BUFFER_BYTES));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = userId(session);
        if (userId == null) {
            return;
        }
        Map<String, WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session.getId());
            if (userSessions.isEmpty()) {
                sessions.remove(userId, userSessions);
            }
        }
    }

    public void send(Collection<Long> recipients, UserEvent event) {
        TextMessage message = new TextMessage(toJson(event));
        for (Long userId : recipients) {
            Map<String, WebSocketSession> userSessions = sessions.get(userId);
            if (userSessions == null) {
                continue;
            }
            for (WebSocketSession session : userSessions.values()) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException | IllegalStateException e) {
                    log.debug("No se pudo enviar el evento al usuario {}: {}", userId, e.getMessage());
                }
            }
        }
    }

    private static Long userId(WebSocketSession session) {
        Principal principal = session.getPrincipal();
        if (principal == null) {
            return null;
        }
        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String toJson(UserEvent event) {
        return "{\"type\":\"" + event.type()
            + "\",\"entity\":\"" + event.entity()
            + "\",\"entityId\":" + event.entityId()
            + ",\"status\":\"" + event.status() + "\"}";
    }
}
