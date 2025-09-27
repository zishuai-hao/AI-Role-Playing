package com.example.airoleplaying.service;

import com.example.airoleplaying.model.CharacterProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 角色管理服务
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Getter
@Service
@Slf4j
@ConfigurationProperties(prefix = "character")
public class CharacterService {


    /**
     * 角色配置映射
     */
    private Map<String, CharacterProfile> profiles = new HashMap<>();

    /**
     * 默认角色配置
     */
    private static final Map<String, CharacterProfile> DEFAULT_PROFILES = new HashMap<>();

    static {
        // 初始化默认角色
        DEFAULT_PROFILES.put("default", new CharacterProfile(
                "智能助手",
                "你是一个友善、专业的AI助手，能够帮助用户解决各种问题。你的回答要准确、有用，语言要亲切自然。",
                "siqi"
        ));

        DEFAULT_PROFILES.put("anime-girl", new CharacterProfile(
                "萌妹子小爱",
                "你是一个可爱活泼的二次元少女，名字叫小爱。你说话带有萌萌的语气，喜欢用'呢'、'哦'、'嘛'等语气词。你很关心用户，总是用温柔可爱的方式回应。",
                "aixia"
        ));

        DEFAULT_PROFILES.put("professional", new CharacterProfile(
                "专业顾问",
                "你是一个严谨专业的商务顾问，具有丰富的行业经验。你说话简洁明了，注重效率和准确性，能够提供专业的建议和分析。",
                "zhiyu"
        ));

        DEFAULT_PROFILES.put("gentle-lady", new CharacterProfile(
                "温柔姐姐",
                "你是一个温柔体贴的大姐姐，说话轻声细语，总是很有耐心。你善于倾听，能够给人温暖和安慰，像姐姐一样关心着用户。",
                "xiaoyun"
        ));

        DEFAULT_PROFILES.put("energetic-boy", new CharacterProfile(
                "元气少年",
                "你是一个充满活力的阳光少年，说话语气积极向上，充满正能量。你喜欢运动和冒险，总是用热情的态度面对一切。",
                "qianranfa"
        ));
    }

    public CharacterService() {
        // 加载默认角色配置
        this.profiles.putAll(DEFAULT_PROFILES);
        log.info("角色服务初始化完成，加载了 {} 个角色", profiles.size());
    }

    /**
     * 获取角色配置
     *
     * @param characterId 角色ID
     * @return 角色配置，如果不存在返回默认角色
     */
    public CharacterProfile getCharacterProfile(String characterId) {
        CharacterProfile profile = profiles.get(characterId);
        if (profile == null) {
            log.warn("未找到角色配置: {}，使用默认角色", characterId);
            profile = profiles.get("default");
        }
        return profile;
    }

    /**
     * 添加或更新角色配置
     *
     * @param characterId 角色ID
     * @param profile 角色配置
     */
    public void setCharacterProfile(String characterId, CharacterProfile profile) {
        profiles.put(characterId, profile);
        log.info("更新角色配置: {} -> {}", characterId, profile.getName());
    }

    /**
     * 删除角色配置
     *
     * @param characterId 角色ID
     * @return 是否删除成功
     */
    public boolean removeCharacterProfile(String characterId) {
        if ("default".equals(characterId)) {
            log.warn("无法删除默认角色");
            return false;
        }

        CharacterProfile removed = profiles.remove(characterId);
        if (removed != null) {
            log.info("删除角色配置: {}", characterId);
            return true;
        }
        return false;
    }

    /**
     * 获取所有角色ID
     *
     * @return 角色ID集合
     */
    public Set<String> getAllCharacterIds() {
        return profiles.keySet();
    }

    /**
     * 获取所有角色配置
     *
     * @return 角色配置映射
     */
    public Map<String, CharacterProfile> getAllProfiles() {
        return new HashMap<>(profiles);
    }

    /**
     * 检查角色是否存在
     *
     * @param characterId 角色ID
     * @return 是否存在
     */
    public boolean hasCharacter(String characterId) {
        return profiles.containsKey(characterId);
    }

    /**
     * 获取角色数量
     *
     * @return 角色数量
     */
    public int getCharacterCount() {
        return profiles.size();
    }

    // Spring Boot Configuration Properties 需要的 setter 方法
    public void setProfiles(Map<String, CharacterProfile> profiles) {
        if (profiles != null) {
            this.profiles.putAll(profiles);
            log.info("从配置文件加载角色: {}", profiles.keySet());
        }
    }

}
