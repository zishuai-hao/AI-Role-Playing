package com.example.airoleplaying.controller;

import com.example.airoleplaying.service.AiChatService;
import com.example.airoleplaying.service.AlibabaAsrService;
import com.example.airoleplaying.service.AlibabaTtsService;
import com.example.airoleplaying.service.VoiceChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统管理控制器
 * 
 * @author AI Assistant
 * @since 1.0.0
 */
@RestController
@RequestMapping("/system")
@CrossOrigin(origins = "*")
public class SystemController {

    private static final Logger logger = LoggerFactory.getLogger(SystemController.class);

    private final VoiceChatService voiceChatService;
    private final AiChatService aiChatService;
    private final AlibabaAsrService asrService;
    private final AlibabaTtsService ttsService;

    public SystemController(VoiceChatService voiceChatService,
                          AiChatService aiChatService,
                          AlibabaAsrService asrService,
                          AlibabaTtsService ttsService) {
        this.voiceChatService = voiceChatService;
        this.aiChatService = aiChatService;
        this.asrService = asrService;
        this.ttsService = ttsService;
    }

    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("status", "UP");
            response.put("timestamp", System.currentTimeMillis());
            response.put("application", "AI Role Playing System");
            response.put("version", "1.0.0");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("健康检查失败: {}", e.getMessage(), e);
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 系统状态检查
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> services = new HashMap<>();
        
        try {
            // 检查AI服务
            boolean aiStatus = aiChatService.testConnection();
            services.put("ai", createServiceStatus(aiStatus, "通义千问AI服务"));
            
            // 检查ASR服务
            boolean asrStatus = asrService.testConnection();
            services.put("asr", createServiceStatus(asrStatus, "阿里云语音识别服务"));
            
            // 检查TTS服务
            boolean ttsStatus = ttsService.testConnection();
            services.put("tts", createServiceStatus(ttsStatus, "阿里云语音合成服务"));
            
            // 检查整体流程
            boolean pipelineStatus = voiceChatService.testVoiceInteractionPipeline();
            services.put("pipeline", createServiceStatus(pipelineStatus, "语音交互流程"));
            
            response.put("services", services);
            response.put("timestamp", System.currentTimeMillis());
            
            boolean allHealthy = aiStatus && asrStatus && ttsStatus && pipelineStatus;
            response.put("overall_status", allHealthy ? "HEALTHY" : "UNHEALTHY");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("状态检查失败: {}", e.getMessage(), e);
            response.put("error", e.getMessage());
            response.put("overall_status", "ERROR");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 测试AI聊天
     */
    @PostMapping("/test/ai-chat")
    public ResponseEntity<Map<String, Object>> testAiChat(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                message = "你好，请进行一个简单的自我介绍。";
            }
            
            logger.info("测试AI聊天，消息: {}", message);
            
            String aiResponse = aiChatService.getSimpleResponse(message);
            
            response.put("success", true);
            response.put("user_message", message);
            response.put("ai_response", aiResponse);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("AI聊天测试失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 测试TTS语音合成
     */
    @PostMapping("/test/tts")
    public ResponseEntity<Map<String, Object>> testTts(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String text = request.get("text");
            String voice = request.get("voice");
            
            if (text == null || text.trim().isEmpty()) {
                text = "这是一个语音合成测试。";
            }
            
            logger.info("测试TTS，文本: {}, 语音: {}", text, voice);
            
            boolean success = ttsService.testSynthesis(text, voice);
            
            response.put("success", success);
            response.put("text", text);
            response.put("voice", voice);
            response.put("message", success ? "TTS测试成功" : "TTS测试失败");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("TTS测试失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取系统信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("application", "AI Role Playing System");
            response.put("description", "基于阿里云服务的AI角色扮演语音交互系统");
            response.put("version", "1.0.0");
            response.put("author", "AI Assistant");
            
            // 技术栈信息
            Map<String, String> tech = new HashMap<>();
            tech.put("framework", "Spring Boot 3.2.0");
            tech.put("java", "Java 17");
            tech.put("ai_service", "阿里云通义千问 (DashScope)");
            tech.put("asr_service", "阿里云智能语音识别");
            tech.put("tts_service", "阿里云智能语音合成");
            tech.put("websocket", "Spring WebSocket");
            response.put("technology", tech);
            
            // 功能特性
            response.put("features", new String[]{
                "实时语音识别",
                "AI角色扮演对话",
                "多角色语音合成",
                "WebSocket实时通信",
                "角色配置管理"
            });
            
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取系统信息失败: {}", e.getMessage(), e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 创建服务状态对象
     */
    private Map<String, Object> createServiceStatus(boolean status, String description) {
        Map<String, Object> serviceStatus = new HashMap<>();
        serviceStatus.put("status", status ? "UP" : "DOWN");
        serviceStatus.put("description", description);
        serviceStatus.put("timestamp", System.currentTimeMillis());
        return serviceStatus;
    }
}
