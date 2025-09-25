package com.example.airoleplaying.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 阿里云服务配置属性类
 * 
 * @author AI Assistant
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "alibaba.cloud")
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

    // Getters and Setters
    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public AsrProperties getAsr() {
        return asr;
    }

    public void setAsr(AsrProperties asr) {
        this.asr = asr;
    }

    public TtsProperties getTts() {
        return tts;
    }

    public void setTts(TtsProperties tts) {
        this.tts = tts;
    }

    /**
     * ASR (语音识别) 配置属性
     */
    public static class AsrProperties {
        private String appKey;
        private String url = "wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1";
        private String format = "pcm";
        private Integer sampleRate = 16000;
        private Boolean enableIntermediateResult = true;
        private Boolean enablePunctuationPrediction = true;
        private Boolean enableInverseTextNormalization = true;

        // Getters and Setters
        public String getAppKey() {
            return appKey;
        }

        public void setAppKey(String appKey) {
            this.appKey = appKey;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public Integer getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(Integer sampleRate) {
            this.sampleRate = sampleRate;
        }

        public Boolean getEnableIntermediateResult() {
            return enableIntermediateResult;
        }

        public void setEnableIntermediateResult(Boolean enableIntermediateResult) {
            this.enableIntermediateResult = enableIntermediateResult;
        }

        public Boolean getEnablePunctuationPrediction() {
            return enablePunctuationPrediction;
        }

        public void setEnablePunctuationPrediction(Boolean enablePunctuationPrediction) {
            this.enablePunctuationPrediction = enablePunctuationPrediction;
        }

        public Boolean getEnableInverseTextNormalization() {
            return enableInverseTextNormalization;
        }

        public void setEnableInverseTextNormalization(Boolean enableInverseTextNormalization) {
            this.enableInverseTextNormalization = enableInverseTextNormalization;
        }
    }

    /**
     * TTS (语音合成) 配置属性
     */
    public static class TtsProperties {
        private String appKey;
        private String url = "wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1";
        private String format = "wav";
        private Integer sampleRate = 16000;
        private String voice = "siqi";

        // Getters and Setters
        public String getAppKey() {
            return appKey;
        }

        public void setAppKey(String appKey) {
            this.appKey = appKey;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public Integer getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(Integer sampleRate) {
            this.sampleRate = sampleRate;
        }

        public String getVoice() {
            return voice;
        }

        public void setVoice(String voice) {
            this.voice = voice;
        }
    }
}
