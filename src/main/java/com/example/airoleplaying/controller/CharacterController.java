package com.example.airoleplaying.controller;

import com.example.airoleplaying.model.CharacterProfile;
import com.example.airoleplaying.model.CharacterSkill;
import com.example.airoleplaying.service.CharacterService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色管理控制器
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/characters")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CharacterController {

    private static final Logger logger = LoggerFactory.getLogger(CharacterController.class);

    private final CharacterService characterService;

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

    /**
     * 搜索角色
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchCharacters(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String skill,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<CharacterProfile> characters = characterService.searchCharacters(keyword, category, skill);
            
            // 分页处理
            int start = page * size;
            int end = Math.min(start + size, characters.size());
            List<CharacterProfile> pagedCharacters = characters.subList(start, end);

            response.put("success", true);
            response.put("characters", pagedCharacters);
            response.put("total", characters.size());
            response.put("page", page);
            response.put("size", size);
            response.put("hasMore", end < characters.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("搜索角色失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取角色分类列表
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getCategories() {
        Map<String, Object> response = new HashMap<>();

        try {
            Set<String> categories = characterService.getAllCategories();

            response.put("success", true);
            response.put("categories", categories);
            response.put("count", categories.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取角色分类失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取可用技能列表
     */
    @GetMapping("/skills")
    public ResponseEntity<Map<String, Object>> getSkills() {
        Map<String, Object> response = new HashMap<>();

        try {
            Set<String> skills = characterService.getAllSkills();
            Map<String, String> skillDetails = new HashMap<>();
            
            // 添加技能详细描述
            for (CharacterSkill skill : CharacterSkill.values()) {
                skillDetails.put(skill.getName(), skill.getDescription());
            }

            response.put("success", true);
            response.put("skills", skills);
            response.put("skillDetails", skillDetails);
            response.put("count", skills.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取技能列表失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 根据分类获取角色
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getCharactersByCategory(@PathVariable String category) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<CharacterProfile> characters = characterService.getCharactersByCategory(category);

            response.put("success", true);
            response.put("category", category);
            response.put("characters", characters);
            response.put("count", characters.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("根据分类获取角色失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 根据技能获取角色
     */
    @GetMapping("/skill/{skill}")
    public ResponseEntity<Map<String, Object>> getCharactersBySkill(@PathVariable String skill) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<CharacterProfile> characters = characterService.getCharactersBySkill(skill);

            response.put("success", true);
            response.put("skill", skill);
            response.put("characters", characters);
            response.put("count", characters.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("根据技能获取角色失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取热门角色
     */
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularCharacters(
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<CharacterProfile> characters = characterService.getPopularCharacters(limit);

            response.put("success", true);
            response.put("characters", characters);
            response.put("count", characters.size());
            response.put("limit", limit);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取热门角色失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取角色统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getCharacterStatistics() {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> statistics = characterService.getCharacterStatistics();

            response.put("success", true);
            response.put("statistics", statistics);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取角色统计失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 更新角色热度
     */
    @PostMapping("/{characterId}/popularity")
    public ResponseEntity<Map<String, Object>> updateCharacterPopularity(
            @PathVariable String characterId,
            @RequestParam int popularity) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (!characterService.hasCharacter(characterId)) {
                response.put("success", false);
                response.put("error", "角色不存在: " + characterId);
                return ResponseEntity.status(404).body(response);
            }

            characterService.updateCharacterPopularity(characterId, popularity);

            response.put("success", true);
            response.put("character_id", characterId);
            response.put("popularity", popularity);
            response.put("message", "角色热度更新成功");
            response.put("timestamp", System.currentTimeMillis());

            logger.info("角色热度更新成功: {} -> {}", characterId, popularity);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("更新角色热度失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
