package com.example.airoleplaying.service;

import com.example.airoleplaying.handler.VoiceChatWebSocketHandler;
import com.example.airoleplaying.model.CharacterProfile;
import com.example.airoleplaying.model.VoiceChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import java.util.concurrent.CompletableFuture;

/**
 * 语音聊天服务 - 系统核心协调服务
 * 整合ASR、AI Chat、TTS服务，提供完整的语音交互流程
 * 
 * @author AI Assistant
 * @since 1.0.0
 */
@Service
public class VoiceChatService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceChatService.class);

    private final AlibabaAsrService asrService;
    private final AlibabaTtsService ttsService;
    private final AiChatService aiChatService;
    
    // 使用@Lazy避免循环依赖
    @Lazy
    private final VoiceChatWebSocketHandler webSocketHandler;

    public VoiceChatService(AlibabaAsrService asrService,
                          AlibabaTtsService ttsService,
                          AiChatService aiChatService,
                          @Lazy VoiceChatWebSocketHandler webSocketHandler) {
        this.asrService = asrService;
        this.ttsService = ttsService;
        this.aiChatService = aiChatService;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * 初始化会话
     * 
     * @param session 语音聊天会话
     */
    public void initializeSession(VoiceChatSession session) {
        try {
            logger.info("初始化语音聊天会话: {}", session.getSessionId());
            
            // 创建ASR转录器
            boolean asrReady = asrService.createTranscriberForSession(session);
            if (!asrReady) {
                logger.warn("ASR服务初始化失败: {}", session.getSessionId());
            }
            
            logger.info("会话初始化完成: {}", session.getSessionId());
            
        } catch (Exception e) {
            logger.error("初始化会话失败: {}", e.getMessage(), e);
            sendErrorToClient(session, "会话初始化失败: " + e.getMessage());
        }
    }

    /**
     * 更新会话角色
     * 
     * @param session 语音聊天会话
     * @param character 新角色配置
     */
    public void updateCharacter(VoiceChatSession session, CharacterProfile character) {
        try {
            logger.info("更新会话角色: {} -> {}", session.getSessionId(), character.getName());
            session.setCharacterProfile(character);
            
            // 发送角色更新通知
            sendNotificationToClient(session, "character_updated", 
                                   "已切换到角色: " + character.getName());
            
        } catch (Exception e) {
            logger.error("更新角色失败: {}", e.getMessage(), e);
            sendErrorToClient(session, "更新角色失败: " + e.getMessage());
        }
    }

    /**
     * 开始录音
     * 
     * @param session 语音聊天会话
     */
    public void startRecording(VoiceChatSession session) {
        try {
            logger.debug("开始录音: {}", session.getSessionId());
            session.setStatus(VoiceChatSession.SessionStatus.RECORDING);
            session.updateLastActiveTime();
            
        } catch (Exception e) {
            logger.error("开始录音失败: {}", e.getMessage(), e);
            sendErrorToClient(session, "开始录音失败: " + e.getMessage());
        }
    }

    /**
     * 停止录音
     * 
     * @param session 语音聊天会话
     */
    public void stopRecording(VoiceChatSession session) {
        try {
            logger.debug("停止录音: {}", session.getSessionId());
            
            // 停止ASR转录
            asrService.stopTranscription(session.getSessionId());
            
            session.setStatus(VoiceChatSession.SessionStatus.IDLE);
            session.updateLastActiveTime();
            
        } catch (Exception e) {
            logger.error("停止录音失败: {}", e.getMessage(), e);
            sendErrorToClient(session, "停止录音失败: " + e.getMessage());
        }
    }

    /**
     * 处理音频数据
     * 
     * @param session 语音聊天会话
     * @param audioData 音频数据
     */
    public void processAudioData(VoiceChatSession session, byte[] audioData) {
        try {
            session.updateLastActiveTime();
            
            // 发送音频数据到ASR服务
            boolean sent = asrService.sendAudioData(session.getSessionId(), audioData);
            if (!sent) {
                logger.warn("发送音频数据到ASR失败: {}", session.getSessionId());
            }
            
        } catch (Exception e) {
            logger.error("处理音频数据失败: {}", e.getMessage(), e);
            sendErrorToClient(session, "处理音频失败: " + e.getMessage());
        }
    }

    /**
     * 处理ASR中间识别结果
     * 
     * @param session 语音聊天会话
     * @param intermediateResult 中间识别结果
     */
    public void handleIntermediateAsrResult(VoiceChatSession session, String intermediateResult) {
        try {
            logger.debug("ASR中间结果: {} - {}", session.getSessionId(), intermediateResult);
            session.updateLastActiveTime();
            
            // 发送中间结果给前端（可选）
            sendNotificationToClient(session, "asr_intermediate", intermediateResult);
            
        } catch (Exception e) {
            logger.error("处理ASR中间结果失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理ASR最终识别结果
     * 
     * @param session 语音聊天会话
     * @param finalResult 最终识别结果
     */
    public void handleFinalAsrResult(VoiceChatSession session, String finalResult) {
        try {
            logger.info("ASR最终结果: {} - {}", session.getSessionId(), finalResult);
            session.updateLastActiveTime();
            session.setStatus(VoiceChatSession.SessionStatus.GENERATING);
            
            // 发送最终识别结果给前端
            sendNotificationToClient(session, "asr_final", finalResult);
            
            // 异步生成AI回复
            CompletableFuture.runAsync(() -> generateAiResponse(session, finalResult));
            
        } catch (Exception e) {
            logger.error("处理ASR最终结果失败: {}", e.getMessage(), e);
            sendErrorToClient(session, "处理识别结果失败: " + e.getMessage());
        }
    }

    /**
     * 生成AI回复
     * 
     * @param session 语音聊天会话
     * @param userMessage 用户消息
     */
    private void generateAiResponse(VoiceChatSession session, String userMessage) {
        try {
            logger.debug("生成AI回复: {} - {}", session.getSessionId(), userMessage);
            
            // 调用AI服务生成回复
            String aiResponse = aiChatService.getCharacterResponse(userMessage, session.getCharacterProfile());
            
            logger.info("AI回复生成完成: {} - {}", session.getSessionId(), aiResponse);
            
            // 发送AI回复文本给前端
            sendNotificationToClient(session, "ai_response", aiResponse);
            
            // 开始语音合成
            session.setStatus(VoiceChatSession.SessionStatus.SYNTHESIZING);
            synthesizeAiResponse(session, aiResponse);
            
        } catch (Exception e) {
            logger.error("生成AI回复失败: {}", e.getMessage(), e);
            sendErrorToClient(session, "生成回复失败: " + e.getMessage());
            session.setStatus(VoiceChatSession.SessionStatus.IDLE);
        }
    }

    /**
     * 合成AI回复语音
     * 
     * @param session 语音聊天会话
     * @param text AI回复文本
     */
    private void synthesizeAiResponse(VoiceChatSession session, String text) {
        try {
            logger.debug("开始语音合成: {} - {}", session.getSessionId(), text);
            
            // 异步调用TTS服务
            ttsService.synthesizeSpeech(session, text).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("语音合成异常: {}", throwable.getMessage(), throwable);
                    handleTtsError(session, throwable.getMessage());
                } else {
                    logger.debug("语音合成任务完成: {}", session.getSessionId());
                }
            });
            
        } catch (Exception e) {
            logger.error("启动语音合成失败: {}", e.getMessage(), e);
            handleTtsError(session, e.getMessage());
        }
    }

    /**
     * 处理TTS音频数据
     * 
     * @param session 语音聊天会话
     * @param audioData 音频数据
     */
    public void handleTtsAudioData(VoiceChatSession session, byte[] audioData) {
        try {
            session.updateLastActiveTime();
            session.setStatus(VoiceChatSession.SessionStatus.PLAYING);
            
            // 发送音频数据给前端
            webSocketHandler.sendAudioData(session.getWebSocketSession(), audioData);
            
        } catch (Exception e) {
            logger.error("处理TTS音频数据失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理ASR错误
     * 
     * @param session 语音聊天会话
     * @param errorMessage 错误消息
     */
    public void handleAsrError(VoiceChatSession session, String errorMessage) {
        logger.error("ASR错误: {} - {}", session.getSessionId(), errorMessage);
        sendErrorToClient(session, "语音识别失败: " + errorMessage);
        session.setStatus(VoiceChatSession.SessionStatus.IDLE);
    }

    /**
     * 处理TTS错误
     * 
     * @param session 语音聊天会话
     * @param errorMessage 错误消息
     */
    public void handleTtsError(VoiceChatSession session, String errorMessage) {
        logger.error("TTS错误: {} - {}", session.getSessionId(), errorMessage);
        sendErrorToClient(session, "语音合成失败: " + errorMessage);
        session.setStatus(VoiceChatSession.SessionStatus.IDLE);
    }

    /**
     * 清理会话资源
     * 
     * @param session 语音聊天会话
     */
    public void cleanupSession(VoiceChatSession session) {
        try {
            logger.info("清理会话资源: {}", session.getSessionId());
            
            // 清理ASR资源
            asrService.cleanupSession(session.getSessionId());
            
            // 清理TTS资源
            ttsService.cleanupSession(session.getSessionId());
            
            session.setStatus(VoiceChatSession.SessionStatus.CLOSED);
            
            logger.info("会话资源清理完成: {}", session.getSessionId());
            
        } catch (Exception e) {
            logger.error("清理会话资源失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送通知给客户端
     * 
     * @param session 会话
     * @param type 通知类型
     * @param message 通知消息
     */
    private void sendNotificationToClient(VoiceChatSession session, String type, String message) {
        try {
            String notification = createNotificationMessage(type, "success", message);
            webSocketHandler.sendMessage(session.getWebSocketSession(), notification);
        } catch (Exception e) {
            logger.error("发送通知失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送错误消息给客户端
     * 
     * @param session 会话
     * @param error 错误消息
     */
    private void sendErrorToClient(VoiceChatSession session, String error) {
        try {
            String errorMessage = createNotificationMessage("error", "failed", error);
            webSocketHandler.sendMessage(session.getWebSocketSession(), errorMessage);
        } catch (Exception e) {
            logger.error("发送错误消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 创建通知消息
     * 
     * @param type 类型
     * @param status 状态
     * @param message 消息
     * @return JSON字符串
     */
    private String createNotificationMessage(String type, String status, String message) {
        return String.format("{\"type\":\"%s\",\"status\":\"%s\",\"message\":\"%s\",\"timestamp\":%d}",
                           type, status, message, System.currentTimeMillis());
    }

    /**
     * 测试完整的语音交互流程
     * 
     * @return 测试结果
     */
    public boolean testVoiceInteractionPipeline() {
        try {
            logger.info("测试语音交互流程...");
            
            // 测试AI服务
            boolean aiReady = aiChatService.testConnection();
            if (!aiReady) {
                logger.error("AI服务测试失败");
                return false;
            }
            
            // 测试ASR服务
            boolean asrReady = asrService.testConnection();
            if (!asrReady) {
                logger.error("ASR服务测试失败");
                return false;
            }
            
            // 测试TTS服务
            boolean ttsReady = ttsService.testConnection();
            if (!ttsReady) {
                logger.error("TTS服务测试失败");
                return false;
            }
            
            logger.info("语音交互流程测试成功");
            return true;
            
        } catch (Exception e) {
            logger.error("语音交互流程测试失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
