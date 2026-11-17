package cl.udec.cocinavecina.components;

import cl.udec.cocinavecina.dto.UserEvent;

import java.util.Collection;

public record RealtimeEvent(
    Collection<Long> recipients,
    UserEvent event
) {
}
