package com.example.airoleplaying.service;

import com.alibaba.nls.client.AccessToken;
import com.alibaba.nls.client.protocol.InputFormatEnum;
import com.alibaba.nls.client.protocol.OutputFormatEnum;
import com.alibaba.nls.client.protocol.SampleRateEnum;
import com.alibaba.nls.client.protocol.NlsClient;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriber;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberListener;
import com.alibaba.nls.client.protocol.asr.SpeechTranscriberResponse;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizer;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerListener;
import com.alibaba.nls.client.protocol.tts.SpeechSynthesizerResponse;
import com.example.airoleplaying.config.AlibabaCloudProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpeechAiService {
    private final AlibabaCloudProperties alibabaCloudProperties;
    private NlsClient client;
    private volatile String accessToken;
    private static long startTime;

    @PostConstruct
    public void init() {
        try {
            log.info("初始化阿里云语音AI服务...");
            refreshAccessToken();
            client = new NlsClient(accessToken);
            log.info("阿里云语音AI服务初始化成功");
        } catch (Exception e) {
            log.error("阿里云语音AI服务初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI语音服务初始化失败", e);
        }
    }

    private void refreshAccessToken() throws Exception {
        AccessToken accessTokenClient = new AccessToken(
                alibabaCloudProperties.getAccessKeyId(),
                alibabaCloudProperties.getAccessKeySecret()
        );
        accessTokenClient.apply();
        this.accessToken = accessTokenClient.getToken();
        if (this.accessToken == null || this.accessToken.isEmpty()) {
            throw new Exception("获取访问令牌失败");
        }
        log.debug("访问令牌刷新成功");
    }

    /**
     * 语音转文本（ASR）
     */
    public CompletableFuture<String> speechToText(MultipartFile file) {
        CompletableFuture<String> resultFuture = new CompletableFuture<>();
        SpeechTranscriber transcriber = null;
        try {
            transcriber = new SpeechTranscriber(client, getTranscriberListener(resultFuture));
            transcriber.setAppKey(alibabaCloudProperties.getAsr().getAppKey());
            transcriber.setFormat(InputFormatEnum.PCM);
            transcriber.setSampleRate(SampleRateEnum.SAMPLE_RATE_8K);
            transcriber.setEnableIntermediateResult(false);
            transcriber.setEnablePunctuation(true);
            transcriber.setEnableITN(false);
            transcriber.start();
            try (InputStream inputStream = file.getInputStream()) {
                byte[] b = new byte[3200];
                int len;
                while ((len = inputStream.read(b)) > 0) {
                    log.debug("发送数据包长度: {}", len);
                    transcriber.send(b, len);
                    int deltaSleep = getSleepDelta(len, 8000);
                    Thread.sleep(deltaSleep);
                }
            }
            long now = System.currentTimeMillis();
            log.info("ASR等待完成");
            transcriber.stop();
            log.info("ASR延迟: {} ms", (System.currentTimeMillis() - now));
        } catch (Exception e) {
            log.error("语音转录失败: {}", e.getMessage(), e);
            resultFuture.completeExceptionally(new RuntimeException("语音转录失败: " + e.getMessage()));
        } finally {
            if (null != transcriber) {
                transcriber.close();
            }
        }
        return resultFuture;
    }

    private static SpeechTranscriberListener getTranscriberListener(CompletableFuture<String> resultFuture) {
        return new SpeechTranscriberListener() {
            private final StringBuilder fullText = new StringBuilder();
            @Override
            public void onTranscriptionResultChange(SpeechTranscriberResponse response) {
                log.debug("中间结果 - task_id: {}, status: {}, result: {}",
                        response.getTaskId(), response.getStatus(), response.getTransSentenceText());
            }
            @Override
            public void onTranscriberStart(SpeechTranscriberResponse response) {
                log.debug("转录开始 - task_id: {}, status: {}", response.getTaskId(), response.getStatus());
            }
            @Override
            public void onSentenceBegin(SpeechTranscriberResponse response) {
                log.debug("句子开始 - task_id: {}, status: {}", response.getTaskId(), response.getStatus());
            }
            @Override
            public void onSentenceEnd(SpeechTranscriberResponse response) {
                String sentenceText = response.getTransSentenceText();
                if (sentenceText != null && !sentenceText.trim().isEmpty()) {
                    fullText.append(sentenceText).append(" ");
                    log.debug("句子结束 - task_id: {}, text: {}, confidence: {}",
                            response.getTaskId(), sentenceText, response.getConfidence());
                }
            }
            @Override
            public void onTranscriptionComplete(SpeechTranscriberResponse response) {
                log.info("转录完成 - task_id: {}, status: {}", response.getTaskId(), response.getStatus());
                String finalText = fullText.toString().trim();
                if (!finalText.isEmpty()) {
                    resultFuture.complete(finalText);
                } else {
                    resultFuture.completeExceptionally(new RuntimeException("转录结果为空"));
                }
            }
            @Override
            public void onFail(SpeechTranscriberResponse response) {
                log.error("转录失败 - task_id: {}, status: {}, status_text: {}",
                        response.getTaskId(), response.getStatus(), response.getStatusText());
                resultFuture.completeExceptionally(new RuntimeException("转录失败: " + response.getStatusText()));
            }
        };
    }

    private static int getSleepDelta(int dataSize, int sampleRate) {
        return (dataSize * 10 * 8000) / (160 * sampleRate);
    }

    /**
     * 文本转语音（TTS），返回音频字节数组
     */
    public CompletableFuture<byte[]> textToSpeech(String text) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        new Thread(() -> {
            SpeechSynthesizer synthesizer = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                synthesizer = new SpeechSynthesizer(client, getTtsListener(baos, future));
                synthesizer.setAppKey(alibabaCloudProperties.getTts().getAppKey());
                synthesizer.setFormat(OutputFormatEnum.WAV);
                synthesizer.setSampleRate(SampleRateEnum.SAMPLE_RATE_16K);
                synthesizer.setVoice(alibabaCloudProperties.getTts().getVoice());
                synthesizer.setPitchRate(100);
                synthesizer.setSpeechRate(100);
                synthesizer.setText(text);
                synthesizer.addCustomedParam("enable_subtitle", false);
                long start = System.currentTimeMillis();
                synthesizer.start();
                log.info("tts start latency {} ms", (System.currentTimeMillis() - start));
                SpeechAiService.startTime = System.currentTimeMillis();
                synthesizer.waitForComplete();
                log.info("tts stop latency {} ms", (System.currentTimeMillis() - start));
            } catch (Exception e) {
                log.error("TTS合成异常", e);
                future.completeExceptionally(e);
            } finally {
                if (null != synthesizer) {
                    synthesizer.close();
                }
                try { baos.close(); } catch (IOException ignore) {}
            }
        }).start();
        return future;
    }

    private static SpeechSynthesizerListener getTtsListener(ByteArrayOutputStream baos, CompletableFuture<byte[]> future) {
        return new SpeechSynthesizerListener() {
            private boolean firstRecvBinary = true;
            @Override
            public void onComplete(SpeechSynthesizerResponse response) {
                try { baos.close(); } catch (IOException ignore) {}
                log.info("TTS合成完成: {}", response.getName());
                future.complete(baos.toByteArray());
            }
            @Override
            public void onMessage(ByteBuffer message) {
                try {
                    if(firstRecvBinary) {
                        firstRecvBinary = false;
                        long now = System.currentTimeMillis();
                        log.info("tts first latency : {} ms", (now - SpeechAiService.startTime));
                    }
                    byte[] bytesArray = new byte[message.remaining()];
                    message.get(bytesArray, 0, bytesArray.length);
                    baos.write(bytesArray);
                } catch (IOException e) {
                    log.error("TTS写入音频流失败", e);
                    future.completeExceptionally(e);
                }
            }
            @Override
            public void onFail(SpeechSynthesizerResponse response){
                log.error("TTS合成失败: {}", response.getStatusText());
                future.completeExceptionally(new RuntimeException("TTS合成失败: " + response.getStatusText()));
            }
        };
    }
}

