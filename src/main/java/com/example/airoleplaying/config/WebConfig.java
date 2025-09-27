package com.example.airoleplaying.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类 - 配置静态资源处理
 * 
 * @author AI Assistant
 * @since 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源处理
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // 缓存1小时
        
        // 确保HTML文件也能被正确访问
        registry.addResourceHandler("/*.html")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0); // HTML文件不缓存，确保更新及时生效
    }
}
