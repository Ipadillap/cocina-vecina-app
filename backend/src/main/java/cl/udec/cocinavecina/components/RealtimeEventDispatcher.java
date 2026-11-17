package cl.udec.cocinavecina.components;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RealtimeEventDispatcher {
    private final ObjectProvider<EventChannel> channel;

    public RealtimeEventDispatcher(ObjectProvider<EventChannel> channel) {
        this.channel = channel;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEvent(RealtimeEvent event) {
        EventChannel eventChannel = channel.getIfAvailable();
        if (eventChannel != null) {
            eventChannel.send(event.recipients(), event.event());
        }
    }
}
