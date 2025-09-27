package com.example.airoleplaying.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

/**
 * 阿里云服务配置属性类
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "alibaba.cloud")
@Data
public class AlibabaCloudProperties {

    /**
     * 阿里云访问密钥ID
     */
    private String accessKeyId;

    /**
     * 阿里云访问密钥Secret
     */
    private String accessKeySecret;

    /**
     * ASR配置
     */
    @NestedConfigurationProperty
    private AsrProperties asr = new AsrProperties();

    /**
     * TTS配置
     */
    @NestedConfigurationProperty
    private TtsProperties tts = new TtsProperties();


    /**
     * ASR (语音识别) 配置属性
     */
    @Setter
    @Getter
    public static class AsrProperties {
        // Getters and Setters
        private String appKey;
        private String url = "wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1";
        private String format = "pcm";
        private Integer sampleRate = 16000;
        private Boolean enableIntermediateResult = true;
        private Boolean enablePunctuationPrediction = true;
        private Boolean enableInverseTextNormalization = true;

    }

    /**
     * TTS (语音合成) 配置属性
     */
    @Setter
    @Getter
    public static class TtsProperties {
        // Getters and Setters
        private String appKey;
        private String url = "wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1";
        private String format = "wav";
        private Integer sampleRate = 16000;
        private String voice = "siqi";

    }
}
