package com.example.airoleplaying.model;

import lombok.*;

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
}
