package com.example.airoleplaying.config;

import com.example.airoleplaying.handler.VoiceChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 
 * @author AI Assistant
 * @since 1.0.0
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${websocket.path:/ws/voice-chat}")
    private String websocketPath;

    @Value("${websocket.allowed-origins:*}")
    private String allowedOrigins;

    private final VoiceChatWebSocketHandler voiceChatHandler;

    public WebSocketConfig(VoiceChatWebSocketHandler voiceChatHandler) {
        this.voiceChatHandler = voiceChatHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(voiceChatHandler, websocketPath)
                .setAllowedOrigins(allowedOrigins)
                .withSockJS(); // 启用SockJS fallback支持
    }
}
