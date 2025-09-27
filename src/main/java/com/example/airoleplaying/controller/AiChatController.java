/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.airoleplaying.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.example.airoleplaying.model.CharacterProfile;
import com.example.airoleplaying.service.CharacterService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final ChatClient dashScopeChatClient;
    private final CharacterService characterService;

    /**
     * ChatClient 简单调用
     */
    @GetMapping("/simple/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query) {
        return dashScopeChatClient.prompt(query).call().content();
    }

    /**
     * 角色扮演简单对话
     * @param characterId 角色ID，如：default, anime-girl, professional, gentle-lady, energetic-boy
     * @param query 用户问题
     * @return AI回复
     */
    @GetMapping("/roleplay/simple/chat")
    public String roleplaySimpleChat(
            @RequestParam(value = "character", defaultValue = "default") String characterId,
            @RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query) {
        
        CharacterProfile character = characterService.getCharacterProfile(characterId);
        return dashScopeChatClient
                .prompt()
                .system(character.getPersonality())
                .user(query)
                .call()
                .content();
    }

    /**
     * ChatClient 流式调用
     */
    @GetMapping("/stream/chat")
    public Flux<String> streamChat(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return dashScopeChatClient.prompt(query).stream().content();
    }

    /**
     * 角色扮演流式对话
     * @param characterId 角色ID，如：default, anime-girl, professional, gentle-lady, energetic-boy
     * @param query 用户问题
     * @param response HTTP响应
     * @return AI回复流
     */
    @GetMapping("/roleplay/stream/chat")
    public Flux<String> roleplayStreamChat(
            @RequestParam(value = "character", defaultValue = "default") String characterId,
            @RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query, 
            HttpServletResponse response) {
        
        response.setCharacterEncoding("UTF-8");
        CharacterProfile character = characterService.getCharacterProfile(characterId);
        return dashScopeChatClient
                .prompt()
                .system(character.getPersonality())
                .user(query)
                .stream()
                .content();
    }

    /**
     * 语音聊天接口 - 处理语音转录后的文本并返回AI回复
     * @param characterId 角色ID
     * @param message 用户消息（从语音转录获得）
     * @return AI回复
     */
    @GetMapping("/voice/chat")
    public String voiceChat(
            @RequestParam(value = "character", defaultValue = "default") String characterId,
            @RequestParam(value = "message") String message) {
        
        if (message == null || message.trim().isEmpty()) {
            return "抱歉，我没有听到您说什么，请再说一遍。";
        }
        
        CharacterProfile character = characterService.getCharacterProfile(characterId);
        return dashScopeChatClient
                .prompt()
                .system(character.getPersonality())
                .user(message.trim())
                .call()
                .content();
    }

    /**
     * 语音聊天流式接口 - 处理语音转录后的文本并返回AI回复流
     * @param characterId 角色ID
     * @param message 用户消息（从语音转录获得）
     * @param response HTTP响应
     * @return AI回复流
     */
    @GetMapping("/voice/stream/chat")
    public Flux<String> voiceStreamChat(
            @RequestParam(value = "character", defaultValue = "default") String characterId,
            @RequestParam(value = "message") String message,
            HttpServletResponse response) {
        
        response.setCharacterEncoding("UTF-8");
        
        if (message == null || message.trim().isEmpty()) {
            return Flux.just("抱歉，我没有听到您说什么，请再说一遍。");
        }
        
        CharacterProfile character = characterService.getCharacterProfile(characterId);
        return dashScopeChatClient
                .prompt()
                .system(character.getPersonality())
                .user(message.trim())
                .stream()
                .content();
    }
}
