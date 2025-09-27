// 通用录音器，兼容WebM/OGG/WAV，支持采样率转换和WAV导出
class Recorder {
    constructor(options = {}) {
        this.mediaRecorder = null;
        this.audioChunks = [];
        this.audioBlob = null;
        this.audioUrl = null;
        this.isRecording = false;
        this.mimeType = options.mimeType || this.pickSupportedMime();
        this.sampleRate = options.sampleRate || 8000; // 默认8kHz
        this.onStatus = options.onStatus || function(){};
        this.onError = options.onError || function(){};
        this.onReady = options.onReady || function(){};
        this.onStop = options.onStop || function(){};
    }

    pickSupportedMime() {
        const candidates = [
            'audio/webm;codecs=opus',
            'audio/webm',
            'audio/ogg;codecs=opus',
            'audio/mp4',
            'audio/wav'
        ];
        for (const mime of candidates) {
            if (window.MediaRecorder && MediaRecorder.isTypeSupported && MediaRecorder.isTypeSupported(mime)) {
                return mime;
            }
        }
        return 'audio/webm';
    }

    async start() {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: { channelCount: 1 } });
            this.mediaRecorder = new MediaRecorder(stream, { mimeType: this.mimeType });
            this.audioChunks = [];
            this.mediaRecorder.ondataavailable = (e) => {
                if (e.data.size > 0) this.audioChunks.push(e.data);
            };
            this.mediaRecorder.onstop = async () => {
                const blob = new Blob(this.audioChunks, { type: this.mimeType });
                // 转换为WAV（8kHz/16bit）
                const arrayBuffer = await blob.arrayBuffer();
                const audioCtx = new AudioContext();
                const decoded = await audioCtx.decodeAudioData(arrayBuffer);
                const samples = await this.downsampleBuffer(decoded, this.sampleRate);
                this.audioBlob = this.encodeWAV(samples, this.sampleRate);
                this.audioUrl = URL.createObjectURL(this.audioBlob);
                this.onReady(this.audioBlob, this.audioUrl);
                this.onStop();
                stream.getTracks().forEach(track => track.stop());
            };
            this.mediaRecorder.start();
            this.isRecording = true;
            this.onStatus('recording');
        } catch (err) {
            this.onError(err);
        }
    }

    stop() {
        if (this.mediaRecorder && this.isRecording) {
            this.mediaRecorder.stop();
            this.isRecording = false;
            this.onStatus('stopped');
        }
    }

    play() {
        if (this.audioUrl) {
            const audio = new Audio(this.audioUrl);
            audio.play();
            this.onStatus('playing');
        }
    }

    getBlob() {
        return this.audioBlob;
    }

    getUrl() {
        return this.audioUrl;
    }

    // PCM转WAV (16bit, mono)
    encodeWAV(samples, sampleRate) {
        const buffer = new ArrayBuffer(44 + samples.length * 2);
        const view = new DataView(buffer);
        function writeString(view, offset, string) {
            for (let i = 0; i < string.length; i++) {
                view.setUint8(offset + i, string.charCodeAt(i));
            }
        }
        let offset = 0;
        writeString(view, offset, 'RIFF'); offset += 4;
        view.setUint32(offset, 36 + samples.length * 2, true); offset += 4;
        writeString(view, offset, 'WAVE'); offset += 4;
        writeString(view, offset, 'fmt '); offset += 4;
        view.setUint32(offset, 16, true); offset += 4;
        view.setUint16(offset, 1, true); offset += 2; // PCM
        view.setUint16(offset, 1, true); offset += 2; // mono
        view.setUint32(offset, sampleRate, true); offset += 4;
        view.setUint32(offset, sampleRate * 2, true); offset += 4;
        view.setUint16(offset, 2, true); offset += 2;
        view.setUint16(offset, 16, true); offset += 2;
        writeString(view, offset, 'data'); offset += 4;
        view.setUint32(offset, samples.length * 2, true); offset += 4;
        let pos = offset;
        for (let i = 0; i < samples.length; i++, pos += 2) {
            let s = Math.max(-1, Math.min(1, samples[i]));
            view.setInt16(pos, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
        }
        return new Blob([buffer], { type: 'audio/wav' });
    }

    // 重采样到 targetRate
    async downsampleBuffer(buffer, targetRate = 8000) {
        if (buffer.sampleRate === targetRate) {
            return buffer.getChannelData(0);
        }
        const offlineCtx = new OfflineAudioContext(1, buffer.duration * targetRate, targetRate);
        const source = offlineCtx.createBufferSource();
        source.buffer = buffer;
        source.connect(offlineCtx.destination);
        source.start(0);
        const rendered = await offlineCtx.startRendering();
        return rendered.getChannelData(0);
    }
}

if (typeof window !== 'undefined') {
    window.Recorder = Recorder;
}

