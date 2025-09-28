package com.example.airoleplaying.model;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * 角色配置信息
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CharacterProfile {

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色性格描述和提示词
     */
    private String personality;

    /**
     * 语音发音人
     */
    private String voice;

    /**
     * 角色背景故事
     */
    private String background;

    /**
     * 角色类型（历史人物、文学角色、专业顾问等）
     */
    private String category;

    /**
     * 角色标签，用于搜索和分类
     */
    private List<String> tags;

    /**
     * 角色技能列表
     */
    private List<String> skills;

    /**
     * 角色技能详细描述
     */
    private Map<String, String> skillDescriptions;

    /**
     * 角色专业领域
     */
    private String expertise;

    /**
     * 角色年龄（可选）
     */
    private Integer age;

    /**
     * 角色性别（可选）
     */
    private String gender;

    /**
     * 角色形象描述
     */
    private String appearance;

    /**
     * 角色名言或口头禅
     */
    private String motto;

    /**
     * 角色是否启用
     */
    private Boolean enabled = true;

    /**
     * 角色热度评分（用于推荐）
     */
    private Integer popularity = 0;

    /**
     * 角色创建时间
     */
    private Long createdAt;

    /**
     * 角色更新时间
     */
    private Long updatedAt;

    /**
     * 构造函数 - 兼容旧版本
     */
    public CharacterProfile(String name, String personality, String voice) {
        this.name = name;
        this.personality = personality;
        this.voice = voice;
        this.enabled = true;
        this.popularity = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 检查角色是否具有指定技能
     */
    public boolean hasSkill(String skill) {
        return skills != null && skills.contains(skill);
    }

    /**
     * 获取技能描述
     */
    public String getSkillDescription(String skill) {
        return skillDescriptions != null ? skillDescriptions.get(skill) : null;
    }

    /**
     * 检查角色是否匹配搜索条件
     */
    public boolean matchesSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        
        String lowerKeyword = keyword.toLowerCase();
        return (name != null && name.toLowerCase().contains(lowerKeyword)) ||
               (background != null && background.toLowerCase().contains(lowerKeyword)) ||
               (expertise != null && expertise.toLowerCase().contains(lowerKeyword)) ||
               (tags != null && tags.stream().anyMatch(tag -> tag.toLowerCase().contains(lowerKeyword))) ||
               (skills != null && skills.stream().anyMatch(skill -> skill.toLowerCase().contains(lowerKeyword)));
    }
}
