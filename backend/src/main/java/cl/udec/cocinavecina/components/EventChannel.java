package cl.udec.cocinavecina.components;

import cl.udec.cocinavecina.dto.UserEvent;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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

@Slf4j
@Component
@ConditionalOnWebApplication
public class EventChannel extends TextWebSocketHandler {
    private static final int SEND_TIMEOUT_MS = 5_000;
    private static final int BUFFER_BYTES = 64 * 1024;

    private final Map<Long, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws IOException {
        final Long userId = userId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        sessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
            .put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, SEND_TIMEOUT_MS, BUFFER_BYTES));
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        final Long userId = userId(session);
        if (userId == null) {
            return;
        }
        final Map<String, WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session.getId());
            if (userSessions.isEmpty()) {
                sessions.remove(userId, userSessions);
            }
        }
    }

    public void send(Collection<Long> recipients, UserEvent event) {
        final TextMessage message = new TextMessage(toJson(event));
        for (Long userId : recipients) {
            final Map<String, WebSocketSession> userSessions = sessions.get(userId);
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
        final Principal principal = session.getPrincipal();
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
