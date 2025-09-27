package com.example.airoleplaying.controller;

import com.example.airoleplaying.model.CharacterProfile;
import com.example.airoleplaying.service.CharacterService;
import com.example.airoleplaying.service.SpeechAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 语音聊天控制器
 * 整合语音转录和AI对话功能
 */
@RestController
@RequestMapping("/api/voice-chat")
@RequiredArgsConstructor
@Slf4j
public class VoiceChatController {
    private final SpeechAiService speechAiService;
    private final CharacterService characterService;
    private final ChatClient dashScopeChatClient;

    /**
     * 完整的语音聊天流程：录音 -> 转录 -> AI对话 -> 语音合成
     * @param file 录音文件
     * @param characterId 角色ID
     * @param synthesizeAudio 是否合成语音回复
     * @return 包含转录文本、AI回复和音频数据的结果
     */
    @PostMapping("/process")
    public CompletableFuture<Map<String, Object>> processVoiceChat(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "character", defaultValue = "default") String characterId,
            @RequestParam(value = "synthesizeAudio", defaultValue = "false") boolean synthesizeAudio) {
        log.info("开始处理语音聊天请求，角色: {}", characterId);
        if (file.isEmpty()) {
            return CompletableFuture.completedFuture(createErrorResponse("录音文件为空"));
        }
        // 先转文本
        return speechAiService.speechToText(file)
                .thenCompose(transcribedText -> {
                    if (transcribedText == null || transcribedText.trim().isEmpty()) {
                        return CompletableFuture.completedFuture(createErrorResponse("未能识别到有效语音内容"));
                    }
                    CharacterProfile character = characterService.getCharacterProfile(characterId);
                    // AI回复
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            String aiResponse = dashScopeChatClient
                                    .prompt()
                                    .system(character.getPersonality())
                                    .user(transcribedText.trim())
                                    .call()
                                    .content();
                            Map<String, Object> result = new HashMap<>();
                            result.put("success", true);
                            result.put("transcribedText", transcribedText);
                            result.put("aiResponse", aiResponse);
                            result.put("character", character.getName());
                            result.put("timestamp", System.currentTimeMillis());
                            return result;
                        } catch (Exception e) {
                            log.error("AI回复生成失败: {}", e.getMessage(), e);
                            return createErrorResponse("AI回复生成失败: " + e.getMessage());
                        }
                    }).thenCompose(aiResult -> {
                        if (!Boolean.TRUE.equals(aiResult.get("success")) || !synthesizeAudio) {
                            return CompletableFuture.completedFuture(aiResult);
                        }
                        String aiResponse = (String) aiResult.get("aiResponse");
                        return speechAiService.textToSpeech(aiResponse)
                                .thenApply(audioBytes -> {
                                    aiResult.put("audioData", Base64.getEncoder().encodeToString(audioBytes));
                                    aiResult.put("hasAudio", true);
                                    return aiResult;
                                })
                                .exceptionally(throwable -> {
                                    aiResult.put("hasAudio", false);
                                    aiResult.put("audioError", "语音合成失败: " + throwable.getMessage());
                                    return aiResult;
                                });
                    });
                })
                .exceptionally(throwable -> {
                    log.error("语音聊天处理异常: {}", throwable.getMessage(), throwable);
                    return createErrorResponse("处理失败: " + throwable.getMessage());
                });
    }

    /**
     * 仅进行语音转录
     * @param file 录音文件
     * @return 转录结果
     */
    @PostMapping("/transcribe")
    public CompletableFuture<Map<String, Object>> transcribeVoice(@RequestParam("file") MultipartFile file) {
        log.info("开始语音转录");
        if (file.isEmpty()) {
            return CompletableFuture.completedFuture(createErrorResponse("录音文件为空"));
        }
        return speechAiService.speechToText(file)
                .thenApply(text -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("text", text);
                    response.put("timestamp", System.currentTimeMillis());
                    return response;
                })
                .exceptionally(throwable -> {
                    log.error("语音转录异常: {}", throwable.getMessage(), throwable);
                    return createErrorResponse("转录异常: " + throwable.getMessage());
                });
    }

    /**
     * 仅进行AI对话（基于文本）
     * @param message 用户消息
     * @param characterId 角色ID
     * @return AI回复
     */
    @PostMapping("/chat")
    public Map<String, Object> chatWithAi(
            @RequestParam("message") String message,
            @RequestParam(value = "character", defaultValue = "default") String characterId) {
        log.info("AI对话请求，角色: {}, 消息: {}", characterId, message);
        if (message == null || message.trim().isEmpty()) {
            return createErrorResponse("消息内容为空");
        }
        try {
            CharacterProfile character = characterService.getCharacterProfile(characterId);

            String aiResponse = dashScopeChatClient
                    .prompt()
                    .system(character.getPersonality())
                    .user(message.trim())
                    .call()
                    .content();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("userMessage", message);
            result.put("aiResponse", aiResponse);
            result.put("character", character.getName());
            result.put("timestamp", System.currentTimeMillis());

            log.info("AI对话成功");
            return result;

        } catch (Exception e) {
            log.error("AI对话失败: {}", e.getMessage(), e);
            return createErrorResponse("AI对话失败: " + e.getMessage());
        }
    }

    /**
     * 文本转语音合成
     * @param text 要合成的文本
     * @return 音频文件响应
     */
    @PostMapping("/tts")
    public CompletableFuture<ResponseEntity<byte[]>> tts(@RequestParam("text") String text) {
        return speechAiService.textToSpeech(text)
                .thenApply(bytes -> ResponseEntity.ok().body(bytes))
                .exceptionally(e -> ResponseEntity.badRequest().body(e.getMessage().getBytes()));
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorMessage);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
