package com.example.airoleplaying.controller;

import com.example.airoleplaying.service.SpeechAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/speech")
@RequiredArgsConstructor
@Slf4j
public class SpeechTranscriberController {
    private final SpeechAiService speechAiService;

    /**
     * 语音转录接口
     * @param file 音频文件
     * @return 转录结果
     */
    @PostMapping("/transcribe")
    public CompletableFuture<ResponseEntity<String>> transcribe(@RequestParam("file") MultipartFile file) {
        return speechAiService.speechToText(file)
                .thenApply(text -> ResponseEntity.ok().body(text))
                .exceptionally(e -> ResponseEntity.badRequest().body(e.getMessage()));
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UP");
    }
}
