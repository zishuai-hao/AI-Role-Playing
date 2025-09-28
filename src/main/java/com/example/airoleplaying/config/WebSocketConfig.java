package com.example.airoleplaying.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.example.airoleplaying.controller.VoiceWebSocketHandler;

import lombok.RequiredArgsConstructor;

/**
 * WebSocket配置类
 * 配置语音流式交互的WebSocket端点
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final VoiceWebSocketHandler voiceWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册语音流式交互的WebSocket端点
        registry.addHandler(voiceWebSocketHandler, "/ws/voice-stream")
                .setAllowedOriginPatterns("*") // 允许跨域，生产环境应该限制具体域名
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
