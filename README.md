# AI 角色扮演系统 (AI Role Playing System)

基于阿里云服务的实时语音交互AI角色扮演系统，支持多角色配置、实时语音识别、AI对话生成和语音合成。

## 🎯 项目简介

这是一个创新的AI角色扮演系统，通过集成阿里云的多项AI服务，实现了完整的语音交互闭环。用户可以实时与不同性格的AI角色进行语音对话，体验沉浸式的角色扮演交互。

## 🚀 核心特性

- **🎭 多角色扮演**: 支持自定义AI角色，不同性格和语音风格
- **🎯 角色技能系统**: 每个角色具备独特的技能，支持技能调用和切换
- **🎤 实时语音交互**: 基于WebSocket的低延迟语音通信
- **🧠 智能对话**: 集成阿里云通义千问，提供高质量的AI对话
- **📝 语音识别**: 阿里云智能语音识别，支持实时转录
- **🔊 语音合成**: 阿里云智能语音合成，多种音色选择
- **🏗️ 微服务架构**: RESTful API + WebSocket 架构
- **🔧 易于扩展**: 模块化设计，便于功能扩展

## 📋 环境要求

### 系统要求
- **Java**: JDK 17 或更高版本
- **Maven**: 3.6+ 
- **操作系统**: Windows 10+, macOS 10.15+, Ubuntu 18.04+
- **内存**: 建议 4GB 以上
- **网络**: 稳定的互联网连接（需要访问阿里云服务）

### 阿里云服务要求
- **通义千问**: DashScope API Key
- **智能语音识别**: ASR App Key
- **智能语音合成**: TTS App Key
- **访问密钥**: Access Key ID 和 Access Key Secret

## 🚀 快速开始

### 1. 克隆项目
```bash
git clone <repository-url>
cd AI-Role-Playing
```

### 2. 配置环境变量
创建环境变量配置文件或直接在系统环境变量中设置：

```bash
# 阿里云访问密钥
export ALIBABA_ACCESS_KEY_ID="your_access_key_id"
export ALIBABA_ACCESS_KEY_SECRET="your_access_key_secret"

# 通义千问API密钥
export DASHSCOPE_API_KEY="your_dashscope_api_key"

# 语音识别App Key
export ALIBABA_ASR_APP_KEY="your_asr_app_key"

# 语音合成App Key  
export ALIBABA_TTS_APP_KEY="your_tts_app_key"
```

### 3. 编译项目
```bash
mvn clean compile
```

### 4. 运行应用
```bash
mvn spring-boot:run
```

或者打包后运行：
```bash
mvn clean package
java -jar target/ai-role-playing-1.0.0.jar
```

### 5. 访问应用
- **Web界面**: http://localhost:8080
- **WebSocket端点**: ws://localhost:8080/ws/voice-stream

## 🏗️ 系统架构设计

### 整体架构图

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端Web界面    │    │   WebSocket     │    │   后端服务层     │
│                 │    │   实时通信       │    │                 │
│  - HTML5录音     │◄──►│  - 语音流传输   │◄──►│  - Spring Boot  │
│  - 音频播放      │    │  - 消息路由     │    │  - WebSocket    │
│  - 角色选择      │    │  - 会话管理     │    │  - 流式处理     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                       ┌───────────────────────────────┼───────────────────────────────┐
                       │                               │                               │
              ┌────────▼────────┐              ┌────────▼────────┐              ┌────────▼────────┐
              │   阿里云ASR     │              │   通义千问      │              │   阿里云TTS     │
              │   语音识别      │              │   AI对话       │              │   语音合成      │
              │                 │              │                 │              │                 │
              │ - 实时转录      │              │ - 角色扮演      │              │ - 多音色       │
              │ - 标点预测      │              │ - 上下文理解    │              │ - 流式合成     │
              │ - 逆文本规整    │              │ - 智能回复      │              │ - 音频优化     │
              └─────────────────┘              └─────────────────┘              └─────────────────┘
```

### 技术栈

#### 后端技术栈
- **框架**: Spring Boot 3.5.5
- **AI集成**: Spring AI + 阿里云DashScope
- **WebSocket**: Spring WebSocket
- **语音服务**: 阿里云NLS SDK 2.2.19
- **构建工具**: Maven
- **Java版本**: JDK 17

#### 前端技术栈
- **核心**: HTML5 + JavaScript (ES6+)
- **音频处理**: Web Audio API
- **通信**: WebSocket API
- **UI**: 原生CSS + 响应式设计

### 核心模块设计

#### 1. 控制器层 (Controller)
```
├── AiChatController.java          # AI对话REST API
├── CharacterController.java       # 角色管理API
├── SpeechTranscriberController.java # 语音识别API
├── VoiceChatController.java       # 语音聊天API
└── VoiceWebSocketHandler.java     # WebSocket处理器
```

#### 2. 服务层 (Service)
```
├── ChatService.java              # AI对话服务
├── CharacterService.java         # 角色管理服务
├── SpeechAiService.java          # 语音AI服务
└── StreamingVoiceService.java    # 流式语音处理服务
```

#### 3. 模型层 (Model)
```
├── CharacterProfile.java         # 角色配置模型
├── CharacterSkill.java           # 角色技能枚举
└── WebSocketMessageEntity.java   # WebSocket消息实体
```

#### 4. 配置层 (Config)
```
├── AlibabaCloudProperties.java   # 阿里云配置
├── WebConfig.java               # Web配置
└── WebSocketConfig.java         # WebSocket配置
```

### 数据流设计

#### 语音交互流程
1. **用户录音** → 前端Web Audio API采集音频
2. **音频传输** → WebSocket实时传输音频数据
3. **语音识别** → 阿里云ASR实时转录为文本
4. **AI对话** → 通义千问基于角色设定生成回复
5. **语音合成** → 阿里云TTS将文本转换为语音
6. **音频播放** → 前端播放AI回复的语音

#### 会话管理
- **会话ID**: 每个WebSocket连接分配唯一会话ID
- **状态管理**: 维护语音识别、AI对话、语音合成的状态
- **超时处理**: 自动清理长时间无活动的会话
- **错误恢复**: 网络异常时的重连和状态恢复

### 角色系统设计

#### 角色配置结构
```yaml
character:
  profiles:
    default:                    # 默认角色
      name: "智能助手"
      personality: "友善、专业的AI助手"
      voice: "siqi"
      skills: ["知识问答", "专业咨询"]
    anime-girl:                # 二次元角色
      name: "萌妹子小爱"
      personality: "可爱活泼的二次元少女"
      voice: "aixia"
      skills: ["情感支持", "创意写作"]
    professional:              # 专业角色
      name: "专业顾问"
      personality: "严谨专业的商务顾问"
      voice: "zhiyu"
      skills: ["专业咨询", "知识问答"]
```

#### 角色技能系统
每个角色都具备独特的技能，技能系统包含以下特性：

##### 技能类型
- **知识问答**: 回答各种知识性问题
- **情感支持**: 提供情感支持和心理安慰
- **语言学习**: 帮助学习语言和语法
- **专业咨询**: 提供专业领域的建议
- **创意写作**: 协助创意写作和表达
- **历史讲解**: 生动讲解历史事件和人物
- **哲学思辨**: 引导哲学思考和讨论
- **文学赏析**: 分析文学作品和提升文学素养
- **科学探索**: 解释科学原理和现象
- **艺术指导**: 提供艺术创作指导

##### 技能调用机制
1. **技能验证**: 检查角色是否具备请求的技能
2. **提示词生成**: 根据技能类型生成相应的引导提示
3. **角色代入**: AI以角色身份使用特定技能回应
4. **状态管理**: 维护技能使用状态和上下文

#### 角色扩展机制
- **动态加载**: 支持运行时添加新角色
- **个性化**: 每个角色独立的性格、语音和技能
- **上下文保持**: 角色在对话中保持一致性
- **技能组合**: 支持角色具备多种技能组合

## 📡 API接口文档

### REST API

#### 1. 角色管理
```http
GET /api/characters
# 获取所有可用角色

GET /api/characters/{characterId}
# 获取指定角色信息

POST /api/characters
# 创建新角色
```

#### 2. AI对话
```http
POST /api/chat
Content-Type: application/json

{
  "message": "用户消息",
  "characterId": "anime-girl",
  "sessionId": "session-uuid"
}
```

#### 3. 角色技能管理
```http
GET /api/characters/skills
# 获取所有可用技能列表

GET /api/characters/skill/{skill}
# 获取具备指定技能的角色列表

POST /api/characters/{characterId}/skills
# 为角色添加技能
Content-Type: application/json

{
  "skills": ["知识问答", "情感支持"]
}
```

### WebSocket API

#### 连接端点
```
ws://localhost:8080/ws/voice-stream
```

#### 消息格式
```json
{
  "type": "audio|text|command",
  "data": "base64编码的音频数据或文本",
  "characterId": "角色ID",
  "sessionId": "会话ID",
  "timestamp": 1234567890
}
```

#### 消息类型
- **audio**: 音频数据消息
- **text**: 文本消息
- **command**: 控制命令（开始/结束录音等）
- **status**: 状态消息
- **use_skill**: 技能使用消息
- **skill_response**: 技能响应消息
- **change_character**: 角色切换消息

## 🎮 使用示例

### 1. 基础语音对话
```javascript
// 连接WebSocket
const ws = new WebSocket('ws://localhost:8080/ws/voice-stream');

// 发送音频数据
ws.send(JSON.stringify({
  type: 'audio',
  data: base64AudioData,
  characterId: 'anime-girl'
}));
```

### 2. 角色切换
```javascript
// 切换到专业顾问角色
ws.send(JSON.stringify({
  type: 'command',
  data: 'switch_character',
  characterId: 'professional'
}));
```

### 3. 文本对话
```javascript
// 发送文本消息
ws.send(JSON.stringify({
  type: 'text',
  data: '你好，请介绍一下自己',
  characterId: 'default'
}));
```

### 4. 技能调用
```javascript
// 使用角色技能
ws.send(JSON.stringify({
  type: 'use_skill',
  skill: '知识问答',
  sessionId: 'session-uuid'
}));

// 接收技能响应
ws.onmessage = function(event) {
  const message = JSON.parse(event.data);
  if (message.type === 'skill_response') {
    console.log(`[${message.skill}] ${message.data}`);
    // 输出: [知识问答] 作为智能助手，我很乐意回答您的知识性问题...
  }
};
```

### 5. 角色切换
```javascript
// 切换到专业顾问角色
ws.send(JSON.stringify({
  type: 'change_character',
  character: 'professional',
  sessionId: 'session-uuid'
}));
```

## 🔧 配置说明

### 应用配置 (application.yml)
```yaml
# 服务器配置
server:
  port: 8080

# Spring AI配置
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-turbo
          temperature: 0.7

# 阿里云服务配置
alibaba:
  cloud:
    access-key-id: ${ALIBABA_ACCESS_KEY_ID}
    access-key-secret: ${ALIBABA_ACCESS_KEY_SECRET}
    asr:
      app-key: ${ALIBABA_ASR_APP_KEY}
    tts:
      app-key: ${ALIBABA_TTS_APP_KEY}
```

### 环境变量配置
| 变量名 | 说明 | 示例 |
|--------|------|------|
| `ALIBABA_ACCESS_KEY_ID` | 阿里云访问密钥ID | LTAI5t... |
| `ALIBABA_ACCESS_KEY_SECRET` | 阿里云访问密钥Secret | xxxx... |
| `DASHSCOPE_API_KEY` | 通义千问API密钥 | sk-xxx... |
| `ALIBABA_ASR_APP_KEY` | 语音识别应用Key | xxxx... |
| `ALIBABA_TTS_APP_KEY` | 语音合成应用Key | xxxx... |

## 🚀 部署指南

### Docker部署
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/ai-role-playing-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 生产环境配置
1. **安全配置**: 设置具体的CORS域名
2. **日志配置**: 调整日志级别和输出格式
3. **监控配置**: 启用Spring Boot Actuator监控
4. **负载均衡**: 配置Nginx反向代理

## 🐛 故障排除

### 常见问题

#### 1. 连接失败
- 检查环境变量是否正确设置
- 确认网络连接正常
- 验证阿里云服务密钥有效性

#### 2. 语音识别失败
- 检查音频格式和采样率
- 确认ASR App Key配置正确
- 查看日志中的错误信息

#### 3. AI对话异常
- 验证DashScope API Key
- 检查网络连接到阿里云
- 确认模型参数配置

### 日志查看
```bash
# 查看应用日志
tail -f logs/ai-role-playing.log

# 查看特定级别日志
grep "ERROR" logs/ai-role-playing.log
```

## 📈 性能优化

### 系统优化
- **连接池**: 复用阿里云服务连接
- **缓存机制**: 缓存角色配置和会话状态
- **异步处理**: 使用线程池处理并发请求
- **资源管理**: 及时释放音频流资源

### 网络优化
- **压缩传输**: 启用WebSocket消息压缩
- **批量处理**: 合并小音频片段
- **超时控制**: 设置合理的连接超时时间

## 🔮 未来规划

### 功能扩展
- [x] 角色技能系统 - 支持角色技能调用和切换
- [x] 技能提示词生成 - 根据技能类型生成引导提示
- [x] 角色搜索和筛选 - 支持按技能搜索角色
- [ ] 支持更多语音音色
- [ ] 添加情感识别功能
- [ ] 实现多轮对话记忆
- [ ] 支持自定义角色训练
- [ ] 技能组合和协同 - 支持多技能协同工作
- [ ] 技能学习机制 - 根据用户反馈优化技能表现

### 技术升级
- [ ] 微服务架构改造
- [ ] 容器化部署
- [ ] 分布式会话管理
- [ ] 实时监控告警

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进项目！

## 📞 联系方式

如有问题或建议，请通过以下方式联系：
- 邮箱: [your-email@example.com]
- GitHub Issues: [项目Issues页面]

---

**注意**: 本项目仅用于学习和演示目的，请确保遵守相关服务的使用条款。

