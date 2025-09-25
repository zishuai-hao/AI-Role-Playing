package com.example.airoleplaying.controller;

import com.example.airoleplaying.model.CharacterProfile;
import com.example.airoleplaying.service.AiChatService;
import com.example.airoleplaying.service.CharacterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 角色管理控制器
 * 
 * @author AI Assistant
 * @since 1.0.0
 */
@RestController
@RequestMapping("/characters")
@CrossOrigin(origins = "*")
public class CharacterController {

    private static final Logger logger = LoggerFactory.getLogger(CharacterController.class);

    private final CharacterService characterService;
    private final AiChatService aiChatService;

    public CharacterController(CharacterService characterService, AiChatService aiChatService) {
        this.characterService = characterService;
        this.aiChatService = aiChatService;
    }

    /**
     * 获取所有角色列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCharacters() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, CharacterProfile> profiles = characterService.getAllProfiles();
            
            response.put("success", true);
            response.put("characters", profiles);
            response.put("count", profiles.size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取角色列表失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取指定角色信息
     */
    @GetMapping("/{characterId}")
    public ResponseEntity<Map<String, Object>> getCharacter(@PathVariable String characterId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!characterService.hasCharacter(characterId)) {
                response.put("success", false);
                response.put("error", "角色不存在: " + characterId);
                return ResponseEntity.status(404).body(response);
            }
            
            CharacterProfile profile = characterService.getCharacterProfile(characterId);
            
            response.put("success", true);
            response.put("character_id", characterId);
            response.put("character", profile);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取角色信息失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 创建或更新角色
     */
    @PostMapping("/{characterId}")
    public ResponseEntity<Map<String, Object>> createOrUpdateCharacter(
            @PathVariable String characterId,
            @RequestBody CharacterProfile characterProfile) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证输入
            if (characterProfile.getName() == null || characterProfile.getName().trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "角色名称不能为空");
                return ResponseEntity.status(400).body(response);
            }
            
            if (characterProfile.getPersonality() == null || characterProfile.getPersonality().trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "角色性格描述不能为空");
                return ResponseEntity.status(400).body(response);
            }
            
            boolean isUpdate = characterService.hasCharacter(characterId);
            characterService.setCharacterProfile(characterId, characterProfile);
            
            response.put("success", true);
            response.put("character_id", characterId);
            response.put("character", characterProfile);
            response.put("operation", isUpdate ? "updated" : "created");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("角色{}成功: {} - {}", isUpdate ? "更新" : "创建", characterId, characterProfile.getName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("创建/更新角色失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{characterId}")
    public ResponseEntity<Map<String, Object>> deleteCharacter(@PathVariable String characterId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean deleted = characterService.removeCharacterProfile(characterId);
            
            if (deleted) {
                response.put("success", true);
                response.put("message", "角色删除成功: " + characterId);
                logger.info("角色删除成功: {}", characterId);
            } else {
                response.put("success", false);
                response.put("error", "无法删除角色: " + characterId);
            }
            
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("删除角色失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 测试角色对话
     */
    @PostMapping("/{characterId}/test")
    public ResponseEntity<Map<String, Object>> testCharacterChat(
            @PathVariable String characterId,
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!characterService.hasCharacter(characterId)) {
                response.put("success", false);
                response.put("error", "角色不存在: " + characterId);
                return ResponseEntity.status(404).body(response);
            }
            
            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                message = "你好，请简单介绍一下自己。";
            }
            
            CharacterProfile character = characterService.getCharacterProfile(characterId);
            String aiResponse = aiChatService.getCharacterResponse(message, character);
            
            response.put("success", true);
            response.put("character_id", characterId);
            response.put("character_name", character.getName());
            response.put("user_message", message);
            response.put("ai_response", aiResponse);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("角色对话测试成功: {} - {}", characterId, character.getName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("角色对话测试失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取可用的语音列表
     */
    @GetMapping("/voices")
    public ResponseEntity<Map<String, Object>> getAvailableVoices() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 阿里云TTS支持的部分语音列表
            Map<String, String> voices = new HashMap<>();
            voices.put("siqi", "思琪 - 温柔女声");
            voices.put("aixia", "艾夏 - 可爱女声");  
            voices.put("xiaoyun", "小云 - 亲切女声");
            voices.put("zhiyu", "智宇 - 成熟男声");
            voices.put("qianranfa", "千然发 - 青年男声");
            voices.put("xiaomeng", "小萌 - 萝莉音");
            voices.put("aiqi", "艾琪 - 甜美女声");
            voices.put("aijing", "艾静 - 知性女声");
            voices.put("xiaoxue", "小雪 - 温暖女声");
            voices.put("xiaokang", "小康 - 阳光男声");
            
            response.put("success", true);
            response.put("voices", voices);
            response.put("count", voices.size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取语音列表失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
