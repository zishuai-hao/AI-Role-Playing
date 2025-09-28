package com.example.airoleplaying.service;

import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.SpeechReqProtocol;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizer;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerListener;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerResponse;
import com.example.airoleplaying.model.CharacterProfile;
import com.example.airoleplaying.model.WebSocketMessageEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;

/**
 * 流式语音处理服务
 * 支持实时语音转录、AI对话和语音合成
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingVoiceService {
    private final SpeechAiService service;
    private final CharacterService characterService;
    private final ChatClient dashScopeChatClient;
    private final ObjectMapper om;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    // 存储活跃的会话和对应的处理器
    private final ConcurrentHashMap<String, SessionContext> activeSessions = new ConcurrentHashMap<>();
    
    // 存储分块音频数据
    private final ConcurrentHashMap<String, Map<Integer, byte[]>> audioChunks = new ConcurrentHashMap<>();
    
    // 存储会话的保持连接定时器
    private final ConcurrentHashMap<String, ScheduledExecutorService> keepAliveTimers = new ConcurrentHashMap<>();
    
    // 会话超时时间（毫秒）
    private static final long SESSION_TIMEOUT = 300000; // 5分钟
    private static final long KEEP_ALIVE_INTERVAL = 10000; // 10秒发送一次保持连接数据
    
    // 添加关闭钩子，确保资源正确释放
    @jakarta.annotation.PreDestroy
    public void destroy() {
        log.info("正在关闭StreamingVoiceService...");
        
        // 关闭所有活跃会话
        activeSessions.keySet().forEach(this::endVoiceSession);
        
        // 关闭所有保持连接定时器
        keepAliveTimers.values().forEach(timer -> {
            timer.shutdown();
            try {
                if (!timer.awaitTermination(1, TimeUnit.SECONDS)) {
                    timer.shutdownNow();
                }
            } catch (InterruptedException e) {
                timer.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
        
        // 关闭线程池
        executorService.shutdown();
        scheduledExecutor.shutdown();
        try {
            if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("StreamingVoiceService已关闭");
    }

    // 启动超时检测任务
    @jakarta.annotation.PostConstruct
    public void startTimeoutChecker() {
        scheduledExecutor.scheduleWithFixedDelay(this::checkTimeoutSessions, 60, 60, TimeUnit.SECONDS);
    }
    
    /**
     * 检查超时会话
     */
    private void checkTimeoutSessions() {
        long currentTime = System.currentTimeMillis();
        activeSessions.entrySet().removeIf(entry -> {
            SessionContext context = entry.getValue();
            if (currentTime - context.lastActivityTime > SESSION_TIMEOUT) {
                log.info("会话超时，自动清理: {}", entry.getKey());
                try {
                    endVoiceSession(entry.getKey());
                } catch (Exception e) {
                    log.error("清理超时会话失败: {}", e.getMessage(), e);
                }
                return true;
            }
            return false;
        });
    }

    /**
     * 开始新的语音会话
     */
    public void startVoiceSession(String sessionId, WebSocketSession webSocketSession, String characterId) {
        log.info("开始语音会话: {}, 角色: {}", sessionId, characterId);

        SessionContext context = new SessionContext();
        context.sessionId = sessionId;
        context.webSocketSession = webSocketSession;
        context.characterId = characterId;
        context.character = characterService.getCharacterProfile(characterId);
        context.lastActivityTime = System.currentTimeMillis(); // 初始化活动时间

        // 初始化语音转录器
        try {
            context.transcriber = new SpeechTranscriber(service.getClient(), createTranscriberListener(sessionId));
            context.transcriber.setAppKey(service.getAlibabaCloudProperties().getAsr().getAppKey());
            context.transcriber.setFormat(InputFormatEnum.PCM);
            context.transcriber.setSampleRate(SampleRateEnum.SAMPLE_RATE_8K);
            context.transcriber.setEnableIntermediateResult(true); // 启用中间结果
            context.transcriber.setEnablePunctuation(true);
            context.transcriber.setEnableITN(false);
            context.transcriber.start();

            activeSessions.put(sessionId, context);
            
            // 启动保持连接定时器
            startKeepAliveTimer(sessionId);
            
            sendMessage(sessionId, WebSocketMessageEntity.createStatus(sessionId, "服务器连接成功，请点击麦克风按钮开始说话"));

        } catch (Exception e) {
            log.error("启动语音会话失败: {}", e.getMessage(), e);
            sendMessage(sessionId, WebSocketMessageEntity.createError(sessionId, "启动语音会话失败: " + e.getMessage()));
        }
    }

    /**
     * 处理音频数据块
     */
    public void processAudioChunk(String sessionId, byte[] audioData) {
        SessionContext context = activeSessions.get(sessionId);
        if (context == null || context.transcriber == null) {
            log.warn("会话不存在或转录器未初始化: {}", sessionId);
            return;
        }
        
        // 检查转录器状态，避免向已关闭的转录器发送数据
        SpeechTranscriber.State state = context.transcriber.getState();
        if (state == SpeechTranscriber.State.STATE_CLOSED || state == SpeechTranscriber.State.STATE_STOP_SENT) {
            log.warn("转录器状态异常({})，丢弃音频数据: {}", state, sessionId);
            sendMessage(sessionId, WebSocketMessageEntity.createError(sessionId, "语音会话已结束，请重新开始会话"));
            return;
        }
        
        try {
            // 检查音频数据大小，避免发送过小的数据块
            if (audioData.length < 100) {
                log.debug("音频数据块太小，跳过: {} bytes", audioData.length);
                return;
            }
            context.transcriber.send(audioData, audioData.length);
            log.debug("发送音频数据块: {} bytes, 转录器状态: {}", audioData.length, state);
            // 更新最后活动时间
            context.lastActivityTime = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("发送音频数据失败: {}", e.getMessage(), e);
            sendMessage(sessionId, WebSocketMessageEntity.createError(sessionId, "音频数据处理失败: " + e.getMessage()));
        }
    }
    
    /**
     * 处理分块音频数据
     */
    public void processAudioChunk(String sessionId, byte[] audioData, String chunkId, Integer chunkIndex, Integer totalChunks) {
        try {
            // 存储分块数据
            String chunkKey = sessionId + "_" + chunkId;
            Map<Integer, byte[]> chunks = audioChunks.computeIfAbsent(chunkKey, k -> new HashMap<>());
            chunks.put(chunkIndex, audioData);
            
            log.debug("收到音频分块: sessionId={}, chunkId={}, chunkIndex={}/{}, size={} bytes", 
                     sessionId, chunkId, chunkIndex, totalChunks, audioData.length);
            
            // 检查是否收到所有分块
            if (chunks.size() == totalChunks) {
                // 合并所有分块
                byte[] completeAudioData = mergeAudioChunks(chunks, totalChunks);
                
                // 处理完整的音频数据
                processAudioChunk(sessionId, completeAudioData);
                
                // 清理分块数据
                audioChunks.remove(chunkKey);
                
                log.info("音频分块合并完成: sessionId={}, totalSize={} bytes", sessionId, completeAudioData.length);
            }
            
        } catch (Exception e) {
            log.error("处理分块音频数据失败: {}", e.getMessage(), e);
            sendMessage(sessionId, WebSocketMessageEntity.createError(sessionId, "分块音频数据处理失败: " + e.getMessage()));
        }
    }
    
    /**
     * 合并音频分块
     */
    private byte[] mergeAudioChunks(Map<Integer, byte[]> chunks, int totalChunks) {
        int totalSize = chunks.values().stream().mapToInt(chunk -> chunk.length).sum();
        byte[] result = new byte[totalSize];
        int offset = 0;
        
        for (int i = 0; i < totalChunks; i++) {
            byte[] chunk = chunks.get(i);
            if (chunk != null) {
                System.arraycopy(chunk, 0, result, offset, chunk.length);
                offset += chunk.length;
            }
        }
        
        return result;
    }

    /**
     * 结束语音会话
     */
    public void endVoiceSession(String sessionId) {
        log.info("结束语音会话: {}", sessionId);

        SessionContext context = activeSessions.remove(sessionId);
        if (context != null) {
            try {
                log.info("[endVoiceSession] 停止保持连接定时器: {}", sessionId);
                // 停止保持连接定时器
                stopKeepAliveTimer(sessionId);
                // 安全关闭转录器
                if (context.transcriber != null) {
                    try {
                        log.info("[endVoiceSession] 停止转录器: {}", sessionId);
                        context.transcriber.stop();
                    } catch (Exception e) {
                        log.warn("停止转录器失败: {}", e.getMessage());
                    }
                    try {
                        log.info("[endVoiceSession] 关闭转录器: {}", sessionId);
                        context.transcriber.close();
                    } catch (Exception e) {
                        log.warn("关闭转录器失败: {}", e.getMessage());
                    }
                }
                // 注意：synthesizer字段已移除，每次合成时创建新实例，无需在此处关闭
                // 检查WebSocket连接状态
                if (context.webSocketSession != null && context.webSocketSession.isOpen()) {
                    sendMessage(sessionId, WebSocketMessageEntity.createStatus(sessionId, "语音会话已结束"));
                }
            } catch (Exception e) {
                log.error("关闭语音会话资源失败: {}", e.getMessage(), e);
            }
        } else {
            log.warn("尝试结束不存在的会话: {}", sessionId);
        }
    }

    /**
     * 创建语音转录监听器
     */
    private SpeechTranscriberListener createTranscriberListener(String sessionId) {
        return new SpeechTranscriberListener() {
            @Override
            public void onTranscriptionResultChange(SpeechTranscriberResponse response) {
                String text = response.getTransSentenceText();
                if (text != null && !text.trim().isEmpty()) {
                    sendMessage(sessionId, WebSocketMessageEntity.createTranscriptionResult(sessionId, text, false));
                    log.debug("中间转录结果: {}", text);
                }
            }

            @Override
            public void onTranscriberStart(SpeechTranscriberResponse response) {
                log.debug("转录开始: {}", response.getTaskId());
            }

            @Override
            public void onSentenceBegin(SpeechTranscriberResponse response) {
                log.debug("句子开始: {}", response.getTaskId());
            }

            @Override
            public void onSentenceEnd(SpeechTranscriberResponse response) {
                String text = response.getTransSentenceText();
                if (text != null && !text.trim().isEmpty()) {
                    sendMessage(sessionId, WebSocketMessageEntity.createTranscriptionResult(sessionId, text, true));
                    log.debug("句子结束: {}", text);

                    // 只在本轮未触发过AI且为最终一句时触发AI
                    SessionContext context = activeSessions.get(sessionId);
                    if (context != null && !context.aiTriggered) {
                        context.aiTriggered = true;
                        log.info("[onSentenceEnd] 触发AI, sessionId={}", sessionId);
                        
                        // 用户说完话后立即关闭ASR，避免超时
                        // 使用异步方式关闭，避免阻塞回调线程
                        executorService.submit(() -> {
                            try {
                                log.info("[onSentenceEnd] 异步关闭ASR开始: {}", sessionId);
                                stopTranscriber(sessionId);
                                log.info("[onSentenceEnd] 异步关闭ASR完成: {}", sessionId);
                            } catch (Exception e) {
                                log.error("[onSentenceEnd] 异步关闭ASR失败: {}", e.getMessage(), e);
                            }
                        });
                        
                        triggerAiConversation(sessionId, text);
                    }
                }
            }

            @Override
            public void onTranscriptionComplete(SpeechTranscriberResponse response) {
                log.info("转录完成: {}", response.getTaskId());
                sendMessage(sessionId, WebSocketMessageEntity.createStatus(sessionId, "语音识别完成"));
                // 兜底：如果没有在onSentenceEnd触发AI，这里再触发一次（极端情况下）
                SessionContext context = activeSessions.get(sessionId);
                if (context != null && !context.aiTriggered) {
                    context.aiTriggered = true;
                    String text = response.getTransSentenceText();
                    if (text != null && !text.trim().isEmpty()) {
                        log.info("[onTranscriptionComplete] 兜底触发AI, sessionId={}", sessionId);
                        triggerAiConversation(sessionId, text);
                    }
                }
            }

            @Override
            public void onFail(SpeechTranscriberResponse response) {
                log.error("转录失败: {}", response.getStatusText());
                sendMessage(sessionId, WebSocketMessageEntity.createError(sessionId, "语音识别失败: " + response.getStatusText()));
            }
        };
    }

    /**
     * 处理用户文本（从WebSocket直接发送的文本）
     */
    public void processUserText(String sessionId, String userText) {
        log.info("处理用户文本: {}", userText);
        
        // 在AI处理期间保持ASR连接活跃
        SessionContext context = activeSessions.get(sessionId);
        if (context != null && context.transcriber != null) {
            // 发送静音数据保持连接活跃
            sendKeepAliveData(sessionId);
        }
        
        triggerAiConversation(sessionId, userText);
    }
    
    /**
     * 发送保持连接活跃的数据
     */
    private void sendKeepAliveData(String sessionId) {
        SessionContext context = activeSessions.get(sessionId);
        if (context == null || context.transcriber == null) {
            log.warn("[sendKeepAliveData] 会话或转录器无效: {}", sessionId);
            return;
        }
        
        // 检查转录器状态，避免向已关闭的转录器发送数据
        SpeechTranscriber.State state = context.transcriber.getState();
        if (state == SpeechTranscriber.State.STATE_CLOSED || state == SpeechTranscriber.State.STATE_STOP_SENT) {
            log.debug("[sendKeepAliveData] 转录器状态异常({})，停止发送保持连接数据: {}", state, sessionId);
            stopKeepAliveTimer(sessionId);
            return;
        }
        
        try {
            byte[] keepAliveData = new byte[320];
            context.transcriber.send(keepAliveData, keepAliveData.length);
            log.debug("[sendKeepAliveData] 发送保持连接数据: {} bytes, sessionId={}, 状态={}", keepAliveData.length, sessionId, state);
        } catch (Exception e) {
            log.warn("[sendKeepAliveData] 发送保持连接数据失败: {}", e.getMessage());
            // 发送失败时停止保持连接定时器
            stopKeepAliveTimer(sessionId);
        }
    }
    
    /**
     * 启动保持连接定时器
     */
    private void startKeepAliveTimer(String sessionId) {
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleWithFixedDelay(() -> {
            try {
                sendKeepAliveData(sessionId);
            } catch (Exception e) {
                log.warn("[startKeepAliveTimer] 保持连接定时器执行失败: {}", e.getMessage());
            }
        }, KEEP_ALIVE_INTERVAL, KEEP_ALIVE_INTERVAL, TimeUnit.MILLISECONDS);
        keepAliveTimers.put(sessionId, timer);
        log.debug("[startKeepAliveTimer] 启动保持连接定时器: {}", sessionId);
    }
    
    /**
     * 停止保持连接定时器
     */
    private void stopKeepAliveTimer(String sessionId) {
        ScheduledExecutorService timer = keepAliveTimers.remove(sessionId);
        if (timer != null) {
            timer.shutdown();
            try {
                if (!timer.awaitTermination(1, TimeUnit.SECONDS)) {
                    timer.shutdownNow();
                }
            } catch (InterruptedException e) {
                timer.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.debug("[stopKeepAliveTimer] 停止保持连接定时器: {}", sessionId);
        }
    }

    /**
     * 启动ASR转录器
     */
    public void startTranscriber(String sessionId) {
        SessionContext context = activeSessions.get(sessionId);
        if (context == null) {
            log.warn("[startTranscriber] 会话不存在: {}", sessionId);
            return;
        }
        // 如果已有转录器，先关闭
        if (context.transcriber != null) {
            try {
                context.transcriber.stop();
                context.transcriber.close();
            } catch (Exception e) {
                log.warn("[startTranscriber] 关闭旧转录器失败: {}", e.getMessage());
            }
        }
        try {
            context.transcriber = new SpeechTranscriber(service.getClient(), createTranscriberListener(sessionId));
            context.transcriber.setAppKey(service.getAlibabaCloudProperties().getAsr().getAppKey());
            context.transcriber.setFormat(InputFormatEnum.PCM);
            context.transcriber.setSampleRate(SampleRateEnum.SAMPLE_RATE_8K);
            context.transcriber.setEnableIntermediateResult(true);
            context.transcriber.setEnablePunctuation(true);
            context.transcriber.setEnableITN(false);
            context.transcriber.start();
            context.aiTriggered = false;
            startKeepAliveTimer(sessionId);
            sendMessage(sessionId, WebSocketMessageEntity.createStatus(sessionId, "ASR已启动，可以说话"));
            log.info("[startTranscriber] 启动ASR: {}", sessionId);
        } catch (Exception e) {
            log.error("[startTranscriber] 启动ASR失败: {}", e.getMessage(), e);
            sendMessage(sessionId, WebSocketMessageEntity.createError(sessionId, "启动ASR失败: " + e.getMessage()));
        }
    }

    /**
     * 关闭ASR转录器
     */
    public void stopTranscriber(String sessionId) {
        SessionContext context = activeSessions.get(sessionId);
        if (context == null) {
            log.warn("[stopTranscriber] 会话不存在: {}", sessionId);
            return;
        }
        
        if (context.transcriber != null) {
            try {
                // 先停止保持连接定时器，避免在关闭过程中继续发送数据
                stopKeepAliveTimer(sessionId);
                
                // 检查转录器状态
                SpeechTranscriber.State currentState = context.transcriber.getState();
                log.info("[stopTranscriber] 开始关闭ASR: {}, 当前状态: {}", sessionId, currentState);
                
                if (currentState == SpeechTranscriber.State.STATE_CLOSED) {
                    log.info("[stopTranscriber] ASR已关闭: {}", sessionId);
                } else {
                    // 先停止转录
                    if (currentState == SpeechTranscriber.State.STATE_STOP_SENT ||
                        currentState == SpeechTranscriber.State.STATE_CONNECTED) {
                        log.info("[stopTranscriber] 停止转录: {}", sessionId);
                        context.transcriber.stop();
                        
                        // 等待stop操作完成
                        Thread.sleep(200);
                        
                        // 检查stop后的状态
                        SpeechTranscriber.State stopState = context.transcriber.getState();
                        log.info("[stopTranscriber] 停止后状态: {}", stopState);
                    }
                    
                    // 关闭转录器
                    log.info("[stopTranscriber] 关闭转录器: {}", sessionId);
                    context.transcriber.close();
                    
                    // 等待close操作完成
                    Thread.sleep(200);
                    
                    // 最终状态检查
                    SpeechTranscriber.State finalState = context.transcriber.getState();
                    log.info("[stopTranscriber] 最终状态: {}", finalState);
                }
                
                log.info("[stopTranscriber] ASR关闭完成: {}", sessionId);
                
            } catch (Exception e) {
                log.error("[stopTranscriber] 关闭ASR失败: {}", e.getMessage(), e);
            } finally {
                // 确保清理资源
                context.transcriber = null;
                // 确保定时器已停止
                stopKeepAliveTimer(sessionId);
            }
        } else {
            log.debug("[stopTranscriber] 转录器已为空: {}", sessionId);
        }
    }

    /**
     * 切换角色
     */
    public void changeCharacter(String sessionId, String newCharacterId) {
        SessionContext context = activeSessions.get(sessionId);
        if (context == null) {
            log.warn("[changeCharacter] 会话不存在: {}", sessionId);
            return;
        }
        
        try {
            String oldCharacterId = context.characterId;
            context.characterId = newCharacterId;
            context.character = characterService.getCharacterProfile(newCharacterId);
            
            log.info("[changeCharacter] 角色切换成功: {} -> {}", oldCharacterId, newCharacterId);
            sendMessage(sessionId, WebSocketMessageEntity.createStatus(sessionId, 
                String.format("角色已从 %s 切换到 %s", oldCharacterId, newCharacterId)));
            
        } catch (Exception e) {
            log.error("[changeCharacter] 角色切换失败: {}", e.getMessage(), e);
            sendMessage(sessionId, WebSocketMessageEntity.createError(sessionId, "角色切换失败: " + e.getMessage()));
        }
    }

    /**
     * 触发AI对话
     */
    private void triggerAiConversation(String sessionId, String userText) {
        SessionContext context = activeSessions.get(sessionId);
        if (context == null) {
            return;
        }

        executorService.submit(() -> {
            try {
                sendMessage(sessionId, WebSocketMessageEntity.createStatus(sessionId, "AI正在思考中..."));

                // 获取AI最终回复
                String aiResponse = dashScopeChatClient
                        .prompt()
                        .system(context.character.getPersonality())
                        .user(userText)
                        .call()
                        .content();

                // 立即发送AI文字回复
                sendMessage(sessionId, WebSocketMessageEntity.createAiResponse(sessionId, aiResponse, false));
                
                // 发送语音合成开始状态
                sendMessage(sessionId, WebSocketMessageEntity.createStatus(sessionId, "正在生成语音回复..."));

                // 对回复进行语音合成
                triggerAudioSynthesis(sessionId, aiResponse);

            } catch (Exception e) {
                log.error("AI对话失败: {}", e.getMessage(), e);
                sendMessage(sessionId, WebSocketMessageEntity.createError(sessionId, "AI对话失败: " + e.getMessage()));
            }
        });
    }


    /**
     * 触发语音合成
     */
    private void triggerAudioSynthesis(String sessionId, String text) {
        SessionContext context = activeSessions.get(sessionId);
        if (context == null) {
            return;
        }
        executorService.submit(() -> {
            SpeechSynthesizer synthesizer = null;
            try {
                sendMessage(sessionId, WebSocketMessageEntity.createStatus(sessionId, "正在生成语音回复..."));
                synthesizer = new SpeechSynthesizer(service.getClient(), createTtsListener(sessionId));
                synthesizer.setAppKey(service.getAlibabaCloudProperties().getTts().getAppKey());
                synthesizer.setFormat(OutputFormatEnum.WAV);
                synthesizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
                synthesizer.setVoice(service.getAlibabaCloudProperties().getTts().getVoice());
                synthesizer.setPitchRate(100);
                synthesizer.setSpeechRate(100);
                synthesizer.setText(text);
                synthesizer.addCustomedParam("enable_subtitle", false);
                
                // 将合成器存储到会话上下文中，以便后续停止
                context.synthesizer = synthesizer;
                
                synthesizer.start();
                synthesizer.waitForComplete();
            } catch (Exception e) {
                log.error("语音合成失败: {}", e.getMessage(), e);
                sendMessage(sessionId, WebSocketMessageEntity.createError(sessionId, "语音合成失败: " + e.getMessage()));
            } finally {
                if (synthesizer != null) {
                    try { synthesizer.close(); } catch (Exception e) { log.warn("关闭合成器失败: {}", e.getMessage()); }
                }
                // 清除会话上下文中的合成器引用
                context.synthesizer = null;
                // ASR已在句子结束时关闭，无需再次关闭
                log.debug("[triggerAudioSynthesis] 语音合成完成，ASR已提前关闭");
            }
        });
    }

    /**
     * 创建TTS监听器
     */
    private SpeechSynthesizerListener createTtsListener(String sessionId) {
        return new SpeechSynthesizerListener() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            @Override
            public void onComplete(SpeechSynthesizerResponse response) {
                try {
                    baos.close();
                    byte[] audioBytes = baos.toByteArray();
                    String audioData = Base64.getEncoder().encodeToString(audioBytes);
                    sendMessage(sessionId, WebSocketMessageEntity.createAudioSynthesis(sessionId, audioData, true));
                    sendMessage(sessionId, WebSocketMessageEntity.createStatus(sessionId, "语音合成完成"));
                    log.info("语音合成完成: {}", response.getName());
                } catch (Exception e) {
                    log.error("处理合成音频失败: {}", e.getMessage(), e);
                    sendMessage(sessionId, WebSocketMessageEntity.createError(sessionId, "语音合成处理失败"));
                }
            }

            @Override
            public void onMessage(ByteBuffer message) {
                try {
                    byte[] bytesArray = new byte[message.remaining()];
                    message.get(bytesArray, 0, bytesArray.length);
                    baos.write(bytesArray);

                    // 可以在这里发送部分音频数据实现真正的流式播放
                    // 但需要前端支持流式音频播放
                } catch (IOException e) {
                    log.error("写入音频流失败", e);
                }
            }

            @Override
            public void onFail(SpeechSynthesizerResponse response) {
                log.error("TTS合成失败: {}", response.getStatusText());
                sendMessage(sessionId, WebSocketMessageEntity.createError(sessionId, "语音合成失败: " + response.getStatusText()));
            }
        };
    }

    /**
     * 停止TTS合成
     */
    public void stopTts(String sessionId) {
        SessionContext context = activeSessions.get(sessionId);
        if (context == null) {
            log.warn("会话不存在: {}", sessionId);
            return;
        }
        
        if (context.synthesizer != null) {
            try {
                context.synthesizer.close();
                context.synthesizer = null;
                log.info("TTS合成已停止: {}", sessionId);
                sendMessage(sessionId, WebSocketMessageEntity.createStatus(sessionId, "TTS合成已停止"));
            } catch (Exception e) {
                log.error("停止TTS合成失败: {}", e.getMessage(), e);
                sendMessage(sessionId, WebSocketMessageEntity.createError(sessionId, "停止TTS合成失败: " + e.getMessage()));
            }
        } else {
            log.debug("当前没有进行TTS合成: {}", sessionId);
            sendMessage(sessionId, WebSocketMessageEntity.createStatus(sessionId, "当前没有进行TTS合成"));
        }
    }

    /**
     * 使用角色技能
     */
    public void useSkill(String sessionId, String skill) {
        SessionContext context = activeSessions.get(sessionId);
        if (context == null) {
            log.warn("会话不存在: {}", sessionId);
            return;
        }
        
        try {
            // 获取当前角色信息
            CharacterProfile character = characterService.getCharacterProfile(context.characterId);
            
            // 检查角色是否具有该技能
            if (!character.hasSkill(skill)) {
                String errorMsg = String.format("角色 %s 不具备技能: %s", character.getName(), skill);
                log.warn(errorMsg);
                sendMessage(sessionId, WebSocketMessageEntity.createError(sessionId, errorMsg));
                return;
            }
            
            // 发送技能使用状态
            sendMessage(sessionId, WebSocketMessageEntity.createStatus(sessionId, 
                String.format("正在使用技能: %s", skill)));
            
            // 模拟技能使用（这里可以根据具体需求实现不同的技能逻辑）
            String skillResponse = generateSkillResponse(character, skill);
            
            // 发送技能响应
            sendMessage(sessionId, WebSocketMessageEntity.createSkillResponse(sessionId, skill, skillResponse));
            
            log.info("技能使用成功: sessionId={}, character={}, skill={}", 
                sessionId, character.getName(), skill);
            
        } catch (Exception e) {
            log.error("技能使用失败: {}", e.getMessage(), e);
            sendMessage(sessionId, WebSocketMessageEntity.createError(sessionId, 
                "技能使用失败: " + e.getMessage()));
        }
    }

    /**
     * 生成技能响应
     */
    private String generateSkillResponse(CharacterProfile character, String skill) {
        // 根据技能类型生成不同的响应
        switch (skill) {
            case "知识问答":
                return String.format("作为%s，我很乐意回答您的知识性问题。请告诉我您想了解什么？", character.getName());
            case "情感支持":
                return String.format("作为%s，我会用心倾听您的心声，给您温暖的支持和安慰。", character.getName());
            case "语言学习":
                return String.format("作为%s，我可以帮助您学习语言，提供语法指导和表达建议。", character.getName());
            case "专业咨询":
                return String.format("作为%s，我可以为您提供专业领域的建议和指导。", character.getName());
            case "创意写作":
                return String.format("作为%s，我可以帮助您进行创意写作，提供灵感和创作指导。", character.getName());
            case "历史讲解":
                return String.format("作为%s，我可以生动地为您讲解历史事件和人物。", character.getName());
            case "哲学思辨":
                return String.format("作为%s，我可以引导您进行哲学思考，探讨人生和世界的深层问题。", character.getName());
            case "文学赏析":
                return String.format("作为%s，我可以深入分析文学作品，提升您的文学素养。", character.getName());
            case "科学探索":
                return String.format("作为%s，我可以用通俗易懂的方式解释科学原理，激发您对科学的兴趣。", character.getName());
            case "艺术指导":
                return String.format("作为%s，我可以提供艺术创作指导，培养您的艺术修养。", character.getName());
            default:
                return String.format("作为%s，我准备使用%s技能为您服务。", character.getName(), skill);
        }
    }

    /**
     * 发送WebSocket消息
     */
    private void sendMessage(String sessionId, WebSocketMessageEntity message) {
        SessionContext context = activeSessions.get(sessionId);
        if (context != null && context.webSocketSession != null && context.webSocketSession.isOpen()) {
            try {
                String jsonMessage = om.writeValueAsString(message);
                context.webSocketSession.sendMessage(new TextMessage(jsonMessage));
            } catch (Exception e) {
                log.error("发送WebSocket消息失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 会话上下文
     */
    private static class SessionContext {
        String sessionId;
        WebSocketSession webSocketSession;
        String characterId;
        CharacterProfile character;
        SpeechTranscriber transcriber;
        SpeechSynthesizer synthesizer; // TTS合成器
        long lastActivityTime; // 最后活动时间，用于超时检测
        boolean aiTriggered = false; // 标记本轮是否已触发AI，防止多次触发
    }
}
