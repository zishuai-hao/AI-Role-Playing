package com.example.airoleplaying.controller;

import com.example.airoleplaying.model.WebSocketMessageEntity;
import com.example.airoleplaying.service.StreamingVoiceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

/**
 * WebSocket语音流式交互处理器
 * 处理实时语音数据传输和流式响应
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VoiceWebSocketHandler implements WebSocketHandler {
    
    private final StreamingVoiceService streamingVoiceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket连接已建立: {}", session.getId());
        
        // 发送欢迎消息
        WebSocketMessageEntity welcomeMessage = new WebSocketMessageEntity();
        welcomeMessage.setType("status");
        welcomeMessage.setData("连接已建立，请发送语音数据开始对话");
        welcomeMessage.setTimestamp(System.currentTimeMillis());
        
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcomeMessage)));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            handleTextMessage(session, (TextMessage) message);
        } else if (message instanceof BinaryMessage) {
            handleBinaryMessage(session, (BinaryMessage) message);
        }
    }
    /**
     * 处理文本消息
     */
    private void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        try {
            String payload = message.getPayload();
            log.debug("收到文本消息长度: {} 字符", payload.length());
            
            // 检查消息大小
            if (payload.length() > 100000) { // 100KB限制
                log.warn("消息过大，跳过处理: {} 字符", payload.length());
                sendError(session, "消息过大，请重新发送");
                return;
            }
            
            WebSocketMessageEntity wsMessage = objectMapper.readValue(payload, WebSocketMessageEntity.class);
            
            switch (wsMessage.getType()) {
                case "start_session":
                    handleStartSession(session, wsMessage);
                    break;
                case "end_session":
                    handleEndSession(session, wsMessage);
                    break;
                case "audio_chunk":
                    handleAudioChunk(session, wsMessage);
                    break;
                case "process_text":
                    handleProcessText(session, wsMessage);
                    break;
                case "ping":
                    handlePing(session, wsMessage);
                    break;
                case "start_transcriber":
                    handleStartTranscriber(session, wsMessage);
                    break;
                case "change_character":
                    handleChangeCharacter(session, wsMessage);
                    break;
                case "stop_tts":
                    handleStopTts(session, wsMessage);
                    break;
                default:
                    log.warn("未知的消息类型: {}", wsMessage.getType());
            }
            
        } catch (Exception e) {
            log.error("处理文本消息失败: {}", e.getMessage(), e);
            sendError(session, "消息处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理二进制消息（音频数据）
     */
    private void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        try {
            byte[] audioData = new byte[message.getPayload().remaining()];
            message.getPayload().get(audioData);
            
            log.debug("收到二进制音频数据: {} bytes", audioData.length);
            
            // 从会话属性中获取会话ID和角色
            String sessionId = (String) session.getAttributes().get("sessionId");
            if (sessionId != null) {
                streamingVoiceService.processAudioChunk(sessionId, audioData);
            } else {
                log.warn("未找到会话ID，无法处理音频数据");
                sendError(session, "未找到会话ID，请先开始会话");
            }
            
        } catch (Exception e) {
            log.error("处理二进制消息失败: {}", e.getMessage(), e);
            sendError(session, "音频数据处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理开始会话
     */
    private void handleStartSession(WebSocketSession session, WebSocketMessageEntity message) throws IOException {
        try {
            String sessionId = message.getSessionId();
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }
            
            String characterId = message.getCharacter();
            if (characterId == null || characterId.trim().isEmpty()) {
                characterId = "default";
            }
            
            // 将会话信息存储到WebSocket会话属性中
            session.getAttributes().put("sessionId", sessionId);
            session.getAttributes().put("characterId", characterId);
            
            // 启动语音会话
            streamingVoiceService.startVoiceSession(sessionId, session, characterId);
            
            log.info("语音会话已启动: sessionId={}, characterId={}", sessionId, characterId);
            
        } catch (Exception e) {
            log.error("启动会话失败: {}", e.getMessage(), e);
            sendError(session, "启动会话失败: " + e.getMessage());
        }
    }

    /**
     * 处理结束会话
     */
    private void handleEndSession(WebSocketSession session, WebSocketMessageEntity message) throws IOException {
        try {
            String sessionId = (String) session.getAttributes().get("sessionId");
            if (sessionId != null) {
                streamingVoiceService.endVoiceSession(sessionId);
                
                // 清除会话属性
                session.getAttributes().remove("sessionId");
                session.getAttributes().remove("characterId");
                
                log.info("语音会话已结束: {}", sessionId);
            }
            
        } catch (Exception e) {
            log.error("结束会话失败: {}", e.getMessage(), e);
            sendError(session, "结束会话失败: " + e.getMessage());
        }
    }

    /**
     * 处理音频数据块
     */
    private void handleAudioChunk(WebSocketSession session, WebSocketMessageEntity message) throws IOException {
        try {
            String sessionId = (String) session.getAttributes().get("sessionId");
            if (sessionId == null) {
                sendError(session, "会话未开始，请先发送start_session消息");
                return;
            }
            
            String audioDataBase64 = message.getData();
            if (audioDataBase64 == null || audioDataBase64.trim().isEmpty()) {
                sendError(session, "音频数据为空");
                return;
            }
            
            // 解码Base64音频数据
            byte[] audioData = Base64.getDecoder().decode(audioDataBase64);
            
            // 检查是否是分块数据
            if (message.getChunkId() != null && message.getChunkIndex() != null && message.getTotalChunks() != null) {
                // 处理分块音频数据
                streamingVoiceService.processAudioChunk(sessionId, audioData, message.getChunkId(), 
                                                      message.getChunkIndex(), message.getTotalChunks());
            } else {
                // 处理普通音频数据
                streamingVoiceService.processAudioChunk(sessionId, audioData);
            }
            
        } catch (Exception e) {
            log.error("处理音频数据块失败: {}", e.getMessage(), e);
            sendError(session, "音频数据处理失败: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", exception.getMessage(), exception);
        
        // 清理会话
        String sessionId = (String) session.getAttributes().get("sessionId");
        if (sessionId != null) {
            try {
                streamingVoiceService.endVoiceSession(sessionId);
            } catch (Exception e) {
                log.error("清理会话时发生错误: {}", e.getMessage(), e);
            }
        }
        
        // 清理会话属性
        session.getAttributes().clear();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("WebSocket连接已关闭: {}, 状态: {}", session.getId(), closeStatus);
        
        // 清理会话
        String sessionId = (String) session.getAttributes().get("sessionId");
        if (sessionId != null) {
            try {
                streamingVoiceService.endVoiceSession(sessionId);
            } catch (Exception e) {
                log.error("清理会话时发生错误: {}", e.getMessage(), e);
            }
        }
        
        // 清理会话属性
        session.getAttributes().clear();
    }

    @Override
    public boolean supportsPartialMessages() {
        return true; // 支持部分消息，用于处理大音频文件
    }

    /**
     * 处理文本处理请求
     */
    private void handleProcessText(WebSocketSession session, WebSocketMessageEntity message) throws IOException {
        try {
            String sessionId = (String) session.getAttributes().get("sessionId");
            if (sessionId == null) {
                sendError(session, "会话未开始，请先发送start_session消息");
                return;
            }
            
            String userText = message.getData();
            if (userText == null || userText.trim().isEmpty()) {
                sendError(session, "文本内容为空");
                return;
            }
            
            // 触发AI对话
            streamingVoiceService.processUserText(sessionId, userText);
            
        } catch (Exception e) {
            log.error("处理文本失败: {}", e.getMessage(), e);
            sendError(session, "文本处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理ping心跳消息
     */
    private void handlePing(WebSocketSession session, WebSocketMessageEntity message) throws IOException {
        try {
            // 发送pong响应
            WebSocketMessageEntity pongMessage = new WebSocketMessageEntity();
            pongMessage.setType("pong");
            pongMessage.setData("pong");
            pongMessage.setTimestamp(System.currentTimeMillis());
            
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pongMessage)));
            log.debug("响应ping消息");
            
        } catch (Exception e) {
            log.error("处理ping消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理启动转录器请求
     */
    private void handleStartTranscriber(WebSocketSession session, WebSocketMessageEntity message) throws IOException {
        try {
            String sessionId = (String) session.getAttributes().get("sessionId");
            if (sessionId == null) {
                sendError(session, "会话未开始，请先发送start_session消息");
                return;
            }
            
            // 启动ASR转录器
            streamingVoiceService.startTranscriber(sessionId);
            
        } catch (Exception e) {
            log.error("启动转录器失败: {}", e.getMessage(), e);
            sendError(session, "启动转录器失败: " + e.getMessage());
        }
    }

    /**
     * 处理角色切换请求
     */
    private void handleChangeCharacter(WebSocketSession session, WebSocketMessageEntity message) throws IOException {
        try {
            String sessionId = (String) session.getAttributes().get("sessionId");
            if (sessionId == null) {
                sendError(session, "会话未开始，请先发送start_session消息");
                return;
            }
            
            String newCharacterId = message.getCharacter();
            if (newCharacterId == null || newCharacterId.trim().isEmpty()) {
                sendError(session, "角色ID不能为空");
                return;
            }
            
            // 更新会话属性
            session.getAttributes().put("characterId", newCharacterId);
            
            // 切换角色
            streamingVoiceService.changeCharacter(sessionId, newCharacterId);
            
            log.info("角色切换成功: sessionId={}, newCharacterId={}", sessionId, newCharacterId);
            
        } catch (Exception e) {
            log.error("角色切换失败: {}", e.getMessage(), e);
            sendError(session, "角色切换失败: " + e.getMessage());
        }
    }

    /**
     * 处理停止TTS请求
     */
    private void handleStopTts(WebSocketSession session, WebSocketMessageEntity message) throws IOException {
        try {
            String sessionId = (String) session.getAttributes().get("sessionId");
            if (sessionId == null) {
                sendError(session, "会话未开始，请先发送start_session消息");
                return;
            }
            
            // 停止TTS合成
            streamingVoiceService.stopTts(sessionId);
            
            log.info("TTS停止成功: sessionId={}", sessionId);
            
        } catch (Exception e) {
            log.error("停止TTS失败: {}", e.getMessage(), e);
            sendError(session, "停止TTS失败: " + e.getMessage());
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            WebSocketMessageEntity errorMsg = WebSocketMessageEntity.createError(null, errorMessage);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMsg)));
        } catch (Exception e) {
            log.error("发送错误消息失败: {}", e.getMessage(), e);
        }
    }
}
