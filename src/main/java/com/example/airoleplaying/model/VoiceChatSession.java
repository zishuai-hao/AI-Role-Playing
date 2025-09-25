package com.example.airoleplaying.model;

import org.springframework.web.socket.WebSocketSession;

/**
 * 语音聊天会话信息
 * 
 * @author AI Assistant
 * @since 1.0.0
 */
public class VoiceChatSession {
    
    /**
     * WebSocket会话ID
     */
    private String sessionId;
    
    /**
     * WebSocket会话对象
     */
    private WebSocketSession webSocketSession;
    
    /**
     * 当前角色配置
     */
    private CharacterProfile characterProfile;
    
    /**
     * 会话状态
     */
    private SessionStatus status;
    
    /**
     * 创建时间
     */
    private long createTime;
    
    /**
     * 最后活跃时间
     */
    private long lastActiveTime;

    public VoiceChatSession() {
        this.createTime = System.currentTimeMillis();
        this.lastActiveTime = this.createTime;
        this.status = SessionStatus.IDLE;
    }

    public VoiceChatSession(String sessionId, WebSocketSession webSocketSession, CharacterProfile characterProfile) {
        this();
        this.sessionId = sessionId;
        this.webSocketSession = webSocketSession;
        this.characterProfile = characterProfile;
    }

    /**
     * 更新最后活跃时间
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    public void setWebSocketSession(WebSocketSession webSocketSession) {
        this.webSocketSession = webSocketSession;
    }

    public CharacterProfile getCharacterProfile() {
        return characterProfile;
    }

    public void setCharacterProfile(CharacterProfile characterProfile) {
        this.characterProfile = characterProfile;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    /**
     * 会话状态枚举
     */
    public enum SessionStatus {
        /**
         * 空闲状态
         */
        IDLE,
        
        /**
         * 正在录音
         */
        RECORDING,
        
        /**
         * 正在识别语音
         */
        RECOGNIZING,
        
        /**
         * 正在生成AI回复
         */
        GENERATING,
        
        /**
         * 正在合成语音
         */
        SYNTHESIZING,
        
        /**
         * 正在播放语音
         */
        PLAYING,
        
        /**
         * 会话已关闭
         */
        CLOSED
    }

    @Override
    public String toString() {
        return "VoiceChatSession{" +
                "sessionId='" + sessionId + '\'' +
                ", characterProfile=" + characterProfile +
                ", status=" + status +
                ", createTime=" + createTime +
                ", lastActiveTime=" + lastActiveTime +
                '}';
    }
}
