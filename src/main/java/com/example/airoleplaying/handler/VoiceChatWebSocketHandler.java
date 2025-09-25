package com.example.airoleplaying.handler;

import com.example.airoleplaying.model.CharacterProfile;
import com.example.airoleplaying.model.VoiceChatSession;
import com.example.airoleplaying.service.CharacterService;
import com.example.airoleplaying.service.VoiceChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音聊天WebSocket处理器
 * 
 * @author AI Assistant
 * @since 1.0.0
 */
@Component
public class VoiceChatWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(VoiceChatWebSocketHandler.class);

    private final VoiceChatService voiceChatService;
    private final CharacterService characterService;
    private final ObjectMapper objectMapper;
    
    /**
     * 存储所有活跃的WebSocket会话
     */
    private final ConcurrentHashMap<String, VoiceChatSession> sessions = new ConcurrentHashMap<>();

    public VoiceChatWebSocketHandler(VoiceChatService voiceChatService, 
                                   CharacterService characterService,
                                   ObjectMapper objectMapper) {
        this.voiceChatService = voiceChatService;
        this.characterService = characterService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        logger.info("WebSocket连接已建立: {}", sessionId);
        
        // 创建默认角色的聊天会话
        CharacterProfile defaultCharacter = characterService.getCharacterProfile("default");
        VoiceChatSession chatSession = new VoiceChatSession(sessionId, session, defaultCharacter);
        
        // 存储会话
        sessions.put(sessionId, chatSession);
        
        // 初始化语音服务连接
        voiceChatService.initializeSession(chatSession);
        
        // 发送连接成功消息
        sendMessage(session, createMessage("connection", "established", "连接已建立"));
        
        logger.info("语音聊天会话已创建: {}", chatSession);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        VoiceChatSession chatSession = sessions.get(sessionId);
        
        if (chatSession == null) {
            logger.warn("未找到会话: {}", sessionId);
            return;
        }
        
        chatSession.updateLastActiveTime();
        
        if (message instanceof TextMessage) {
            handleTextMessage(chatSession, (TextMessage) message);
        } else if (message instanceof BinaryMessage) {
            handleBinaryMessage(chatSession, (BinaryMessage) message);
        } else if (message instanceof PongMessage) {
            handlePongMessage(chatSession, (PongMessage) message);
        }
    }

    /**
     * 处理文本消息（控制指令）
     */
    private void handleTextMessage(VoiceChatSession chatSession, TextMessage message) throws IOException {
        String payload = message.getPayload();
        logger.debug("收到文本消息: {}", payload);
        
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String type = jsonNode.get("type").asText();
            
            switch (type) {
                case "character_switch":
                    handleCharacterSwitch(chatSession, jsonNode);
                    break;
                case "start_recording":
                    handleStartRecording(chatSession);
                    break;
                case "stop_recording":
                    handleStopRecording(chatSession);
                    break;
                case "ping":
                    handlePing(chatSession);
                    break;
                default:
                    logger.warn("未知的消息类型: {}", type);
            }
        } catch (Exception e) {
            logger.error("处理文本消息失败: {}", e.getMessage(), e);
            sendErrorMessage(chatSession.getWebSocketSession(), "处理消息失败: " + e.getMessage());
        }
    }

    /**
     * 处理二进制消息（音频数据）
     */
    private void handleBinaryMessage(VoiceChatSession chatSession, BinaryMessage message) {
        logger.debug("收到音频数据，大小: {} bytes", message.getPayloadLength());
        
        try {
            // 将音频数据发送给语音识别服务
            voiceChatService.processAudioData(chatSession, message.getPayload().array());
        } catch (Exception e) {
            logger.error("处理音频数据失败: {}", e.getMessage(), e);
            sendErrorMessage(chatSession.getWebSocketSession(), "处理音频失败: " + e.getMessage());
        }
    }

    /**
     * 处理Pong消息
     */
    private void handlePongMessage(VoiceChatSession chatSession, PongMessage message) {
        logger.debug("收到Pong消息: {}", chatSession.getSessionId());
        chatSession.updateLastActiveTime();
    }

    /**
     * 处理角色切换
     */
    private void handleCharacterSwitch(VoiceChatSession chatSession, JsonNode jsonNode) throws IOException {
        String characterId = jsonNode.get("character").asText();
        logger.info("切换角色: {} -> {}", chatSession.getCharacterProfile().getName(), characterId);
        
        CharacterProfile newCharacter = characterService.getCharacterProfile(characterId);
        if (newCharacter != null) {
            chatSession.setCharacterProfile(newCharacter);
            voiceChatService.updateCharacter(chatSession, newCharacter);
            
            sendMessage(chatSession.getWebSocketSession(), 
                       createMessage("character_switched", "success", 
                                   "已切换到角色: " + newCharacter.getName()));
        } else {
            sendErrorMessage(chatSession.getWebSocketSession(), "未找到角色: " + characterId);
        }
    }

    /**
     * 处理开始录音
     */
    private void handleStartRecording(VoiceChatSession chatSession) throws IOException {
        logger.debug("开始录音: {}", chatSession.getSessionId());
        chatSession.setStatus(VoiceChatSession.SessionStatus.RECORDING);
        voiceChatService.startRecording(chatSession);
        
        sendMessage(chatSession.getWebSocketSession(), 
                   createMessage("recording", "started", "开始录音"));
    }

    /**
     * 处理停止录音
     */
    private void handleStopRecording(VoiceChatSession chatSession) throws IOException {
        logger.debug("停止录音: {}", chatSession.getSessionId());
        voiceChatService.stopRecording(chatSession);
        
        sendMessage(chatSession.getWebSocketSession(), 
                   createMessage("recording", "stopped", "停止录音"));
    }

    /**
     * 处理Ping
     */
    private void handlePing(VoiceChatSession chatSession) throws IOException {
        sendMessage(chatSession.getWebSocketSession(), 
                   createMessage("pong", "success", "pong"));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        logger.error("WebSocket传输错误: {}, 异常: {}", sessionId, exception.getMessage(), exception);
        
        VoiceChatSession chatSession = sessions.get(sessionId);
        if (chatSession != null) {
            voiceChatService.cleanupSession(chatSession);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        logger.info("WebSocket连接已关闭: {}, 状态: {}", sessionId, closeStatus);
        
        VoiceChatSession chatSession = sessions.remove(sessionId);
        if (chatSession != null) {
            chatSession.setStatus(VoiceChatSession.SessionStatus.CLOSED);
            voiceChatService.cleanupSession(chatSession);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 发送消息给客户端
     */
    public void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            logger.error("发送消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送音频数据给客户端
     */
    public void sendAudioData(WebSocketSession session, byte[] audioData) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new BinaryMessage(audioData));
            }
        } catch (IOException e) {
            logger.error("发送音频数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(WebSocketSession session, String error) {
        sendMessage(session, createMessage("error", "failed", error));
    }

    /**
     * 创建JSON消息
     */
    private String createMessage(String type, String status, String message) {
        try {
            return objectMapper.writeValueAsString(new MessageResponse(type, status, message));
        } catch (Exception e) {
            logger.error("创建消息失败: {}", e.getMessage(), e);
            return "{\"type\":\"error\",\"status\":\"failed\",\"message\":\"创建消息失败\"}";
        }
    }

    /**
     * 获取会话
     */
    public VoiceChatSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 消息响应类
     */
    public static class MessageResponse {
        private String type;
        private String status;
        private String message;
        private long timestamp;

        public MessageResponse(String type, String status, String message) {
            this.type = type;
            this.status = status;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
