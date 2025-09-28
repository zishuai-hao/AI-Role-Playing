// 录音器，专门用于实时语音识别
class StreamingRecorder {
    constructor(options = {}) {
        this.audioContext = null;
        this.processor = null;
        this.stream = null;
        this.isRecording = false;
        this.sampleRate = options.sampleRate || 8000; // 默认8kHz，适合语音识别
        this.bufferSize = options.bufferSize || 1024; // 缓冲区大小，减小以减少数据量
        this.onStatus = options.onStatus || function(){};
        this.onError = options.onError || function(){};
        this.onDataAvailable = options.onDataAvailable || function(){}; // 实时数据回调
        this.onStop = options.onStop || function(){};
    }

    async start() {
        try {
            // 获取麦克风权限
            this.stream = await navigator.mediaDevices.getUserMedia({ 
                audio: { 
                    channelCount: 1,
                    sampleRate: this.sampleRate,
                    echoCancellation: true,
                    noiseSuppression: true,
                    autoGainControl: true
                } 
            });
            
            // 创建AudioContext
            this.audioContext = new (window.AudioContext || window.webkitAudioContext)({
                sampleRate: this.sampleRate
            });
            
            // 创建音频源
            const source = this.audioContext.createMediaStreamSource(this.stream);
            
            // 创建ScriptProcessorNode进行实时处理
            this.processor = this.audioContext.createScriptProcessor(this.bufferSize, 1, 1);
            
            // 实时处理音频数据
            this.processor.onaudioprocess = (event) => {
                if (this.isRecording) {
                    const inputBuffer = event.inputBuffer;
                    const inputData = inputBuffer.getChannelData(0);
                    
                    // 转换为16位PCM数据
                    const pcmData = this.floatTo16BitPCM(inputData);
                    
                    // 发送实时数据
                    this.onDataAvailable(pcmData);
                }
            };
            
            // 连接音频处理链
            source.connect(this.processor);
            this.processor.connect(this.audioContext.destination);
            
            this.isRecording = true;
            this.onStatus('recording');
            
        } catch (err) {
            console.error('启动录音失败:', err);
            this.onError(err);
        }
    }

    stop() {
        if (this.isRecording) {
            this.isRecording = false;
            this.onStatus('stopped');
            
            // 清理资源
            if (this.processor) {
                this.processor.disconnect();
                this.processor = null;
            }
            
            if (this.audioContext) {
                this.audioContext.close();
                this.audioContext = null;
            }
            
            if (this.stream) {
                this.stream.getTracks().forEach(track => track.stop());
                this.stream = null;
            }
            
            this.onStop();
        }
    }
    
    // 将Float32Array转换为16位PCM数据
    floatTo16BitPCM(input) {
        const buffer = new ArrayBuffer(input.length * 2);
        const view = new DataView(buffer);
        let offset = 0;
        for (let i = 0; i < input.length; i++, offset += 2) {
            let s = Math.max(-1, Math.min(1, input[i]));
            view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
        }
        return new Uint8Array(buffer);
    }
    
    // 检查浏览器支持
    static isSupported() {
        return !!(window.AudioContext || window.webkitAudioContext) && 
               navigator.mediaDevices && 
               navigator.mediaDevices.getUserMedia;
    }
}

// 导出到全局
if (typeof window !== 'undefined') {
    window.StreamingRecorder = StreamingRecorder;
}
