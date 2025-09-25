package com.example.airoleplaying.config;

import com.example.airoleplaying.service.VoiceChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 系统初始化器
 * 在应用启动后执行系统检查和初始化任务
 * 
 * @author AI Assistant
 * @since 1.0.0
 */
@Component
public class SystemInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SystemInitializer.class);

    private final VoiceChatService voiceChatService;

    public SystemInitializer(VoiceChatService voiceChatService) {
        this.voiceChatService = voiceChatService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("=================================================");
        logger.info("AI Role Playing System 正在启动...");
        logger.info("=================================================");
        
        try {
            // 执行系统检查
            performSystemCheck();
            
            logger.info("=================================================");
            logger.info("AI Role Playing System 启动完成！");
            logger.info("WebSocket 端点: ws://localhost:8080/api/ws/voice-chat");
            logger.info("系统状态检查: http://localhost:8080/api/system/status");
            logger.info("角色管理: http://localhost:8080/api/characters");
            logger.info("=================================================");
            
        } catch (Exception e) {
            logger.error("系统启动检查失败: {}", e.getMessage(), e);
            logger.warn("系统可能无法正常工作，请检查配置和网络连接");
        }
    }

    /**
     * 执行系统检查
     */
    private void performSystemCheck() {
        logger.info("正在执行系统检查...");
        
        try {
            // 测试完整的语音交互流程
            boolean pipelineHealthy = voiceChatService.testVoiceInteractionPipeline();
            
            if (pipelineHealthy) {
                logger.info("✓ 语音交互流程测试通过");
            } else {
                logger.warn("✗ 语音交互流程测试失败，请检查阿里云服务配置");
            }
            
        } catch (Exception e) {
            logger.error("系统检查异常: {}", e.getMessage(), e);
        }
        
        // 输出配置提示
        printConfigurationTips();
    }

    /**
     * 输出配置提示信息
     */
    private void printConfigurationTips() {
        logger.info("");
        logger.info("配置提示:");
        logger.info("1. 请确保设置了正确的阿里云访问密钥:");
        logger.info("   - DASHSCOPE_API_KEY: 通义千问API密钥");
        logger.info("   - ALIBABA_ACCESS_KEY_ID: 阿里云访问密钥ID");
        logger.info("   - ALIBABA_ACCESS_KEY_SECRET: 阿里云访问密钥Secret");
        logger.info("   - ALIBABA_ASR_APP_KEY: 语音识别应用密钥");
        logger.info("   - ALIBABA_TTS_APP_KEY: 语音合成应用密钥");
        logger.info("");
        logger.info("2. 可以通过环境变量或application.yml配置这些参数");
        logger.info("");
        logger.info("3. 如果服务测试失败，请检查:");
        logger.info("   - 网络连接是否正常");
        logger.info("   - API密钥是否有效");
        logger.info("   - 阿里云服务是否已开通");
        logger.info("");
    }
}
