package com.example.airoleplaying.model;

import lombok.Getter;

/**
 * 角色技能枚举
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Getter
public enum CharacterSkill {
    
    /**
     * 知识问答技能
     */
    KNOWLEDGE_QA("知识问答", "能够回答用户关于特定领域的问题，提供专业准确的知识解答"),
    
    /**
     * 情感支持技能
     */
    EMOTIONAL_SUPPORT("情感支持", "能够识别用户情感状态，提供温暖的情感支持和心理安慰"),
    
    /**
     * 语言学习技能
     */
    LANGUAGE_LEARNING("语言学习", "帮助用户学习特定语言，提供语法指导和表达建议"),
    
    /**
     * 专业咨询技能
     */
    PROFESSIONAL_CONSULTING("专业咨询", "提供特定领域的专业建议和指导"),
    
    /**
     * 创意写作技能
     */
    CREATIVE_WRITING("创意写作", "帮助用户进行创意写作，提供灵感和创作指导"),
    
    /**
     * 历史讲解技能
     */
    HISTORY_TEACHING("历史讲解", "生动地讲解历史事件和人物，让历史变得有趣易懂"),
    
    /**
     * 哲学思辨技能
     */
    PHILOSOPHICAL_THINKING("哲学思辨", "引导用户进行哲学思考，探讨人生和世界的深层问题"),
    
    /**
     * 文学赏析技能
     */
    LITERATURE_APPRECIATION("文学赏析", "深入分析文学作品，提升用户的文学素养和审美能力"),
    
    /**
     * 科学探索技能
     */
    SCIENTIFIC_EXPLORATION("科学探索", "用通俗易懂的方式解释科学原理，激发对科学的兴趣"),
    
    /**
     * 艺术指导技能
     */
    ART_GUIDANCE("艺术指导", "提供艺术创作指导，培养用户的艺术修养和创作能力");

    private final String name;
    private final String description;

    CharacterSkill(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * 根据技能名称获取技能枚举
     */
    public static CharacterSkill fromName(String name) {
        for (CharacterSkill skill : values()) {
            if (skill.name.equals(name)) {
                return skill;
            }
        }
        return null;
    }

    /**
     * 检查是否为有效技能名称
     */
    public static boolean isValidSkill(String name) {
        return fromName(name) != null;
    }
}
