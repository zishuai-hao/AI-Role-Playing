package com.example.airoleplaying.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket消息模型
 * 用于客户端和服务端之间的消息通信
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageEntity {
    
    /**
     * 消息类型
     * 客户端发送：audio_chunk, start_session, end_session
     * 服务端推送：transcription_result, ai_response, audio_synthesis, error, status
     */
    private String type;
    
    /**
     * 消息数据内容
     */
    private String data;
    
    /**
     * 会话ID，用于关联同一个会话的消息
     */
    private String sessionId;
    
    /**
     * 角色ID
     */
    private String character;
    
    /**
     * 是否为最终结果
     */
    private Boolean isFinal;
    
    /**
     * 是否正在流式传输
     */
    private Boolean isStreaming;
    
    /**
     * 是否合成完成
     */
    private Boolean isComplete;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 状态信息
     */
    private String status;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 分块ID（用于分块传输）
     */
    private String chunkId;
    
    /**
     * 分块索引
     */
    private Integer chunkIndex;
    
    /**
     * 总分块数
     */
    private Integer totalChunks;

    /**
     * 创建语音数据消息
     */
    public static WebSocketMessageEntity createAudioChunk(String sessionId, String audioData, String character) {
        WebSocketMessageEntity message = new WebSocketMessageEntity();
        message.setType("audio_chunk");
        message.setData(audioData);
        message.setSessionId(sessionId);
        message.setCharacter(character);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }

    /**
     * 创建转录结果消息
     */
    public static WebSocketMessageEntity createTranscriptionResult(String sessionId, String text, boolean isFinal) {
        WebSocketMessageEntity message = new WebSocketMessageEntity();
        message.setType("transcription_result");
        message.setData(text);
        message.setSessionId(sessionId);
        message.setIsFinal(isFinal);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }

    /**
     * 创建AI回复消息
     */
    public static WebSocketMessageEntity createAiResponse(String sessionId, String text, boolean isStreaming) {
        WebSocketMessageEntity message = new WebSocketMessageEntity();
        message.setType("ai_response");
        message.setData(text);
        message.setSessionId(sessionId);
        message.setIsStreaming(isStreaming);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }

    /**
     * 创建语音合成消息
     */
    public static WebSocketMessageEntity createAudioSynthesis(String sessionId, String audioData, boolean isComplete) {
        WebSocketMessageEntity message = new WebSocketMessageEntity();
        message.setType("audio_synthesis");
        message.setData(audioData);
        message.setSessionId(sessionId);
        message.setIsComplete(isComplete);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }

    /**
     * 创建错误消息
     */
    public static WebSocketMessageEntity createError(String sessionId, String error) {
        WebSocketMessageEntity message = new WebSocketMessageEntity();
        message.setType("error");
        message.setSessionId(sessionId);
        message.setError(error);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }

    /**
     * 创建状态消息
     */
    public static WebSocketMessageEntity createStatus(String sessionId, String status) {
        WebSocketMessageEntity message = new WebSocketMessageEntity();
        message.setType("status");
        message.setSessionId(sessionId);
        message.setStatus(status);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
}
