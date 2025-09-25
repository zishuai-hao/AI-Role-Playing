package com.example.airoleplaying.model;

/**
 * 角色配置信息
 * 
 * @author AI Assistant
 * @since 1.0.0
 */
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

    public CharacterProfile() {
    }

    public CharacterProfile(String name, String personality, String voice) {
        this.name = name;
        this.personality = personality;
        this.voice = voice;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPersonality() {
        return personality;
    }

    public void setPersonality(String personality) {
        this.personality = personality;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    @Override
    public String toString() {
        return "CharacterProfile{" +
                "name='" + name + '\'' +
                ", personality='" + personality + '\'' +
                ", voice='" + voice + '\'' +
                '}';
    }
}
