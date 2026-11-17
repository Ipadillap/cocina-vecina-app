package cl.udec.cocinavecina.config;

import cl.udec.cocinavecina.components.EventChannel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@ConditionalOnWebApplication
public class WebSocketConfig implements WebSocketConfigurer {
    private final EventChannel channel;

    public WebSocketConfig(EventChannel channel) {
        this.channel = channel;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(channel, "/v1/events").setAllowedOriginPatterns("*");
    }
}
