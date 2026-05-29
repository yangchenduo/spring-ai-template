# Spring AI Template

[English](README.md)

一个生产级的 Spring Boot + Spring AI 模板项目，所有中间件通过 Docker 统一管理。

## 功能特性

- **多模型 AI 对话** - OpenAI（默认）+ Ollama（可选本地部署），支持同步/流式响应
- **RAG 检索增强生成** - 基于 PGVector 的文档问答
- **知识库管理** - 文档上传、解析、向量化、管理
- **对话记忆** - Redis 持久化会话历史
- **异步文档处理** - RabbitMQ 驱动的文档入库流水线
- **API 文档** - Knife4j / OpenAPI 3 自动生成接口文档

## 技术栈

| 层级 | 技术 |
|------|------|
| 应用框架 | Spring Boot 3.4.5, Spring AI 1.0.0 |
| 运行环境 | Java 21 |
| 大语言模型 | OpenAI / 兼容接口（默认）, Ollama（可选） |
| 向量数据库 | PostgreSQL + pgvector |
| 缓存 / 记忆 | Redis 7 |
| 消息队列 | RabbitMQ 3.13 |
| ORM 框架 | MyBatis-Plus 3.5.9 |
| 对象存储 | MinIO |
| 接口文档 | Knife4j (OpenAPI 3) |
| 容器化 | Docker Compose |

## 项目结构

```
spring-ai-template/
├── spring-ai-template-common/          # 公共模块：DTO、枚举、工具类
│   └── src/main/java/com/template/common/
│       ├── dto/                        # ChatRequest, RagRequest 等数据传输对象
│       ├── enums/                      # AIProvider, ErrorCode 等枚举
│       ├── result/                     # Result, PageResult 统一响应封装
│       └── utils/                      # GlobalExceptionHandler 全局异常处理
│
├── spring-ai-template-app/             # 应用主模块
│   └── src/main/java/com/template/app/
│       ├── config/                     # AI、Redis、RabbitMQ、Web 等配置类
│       ├── entity/                     # 数据库实体
│       ├── mapper/                     # MyBatis-Plus Mapper 接口
│       ├── service/                    # 业务逻辑层
│       │   └── impl/                   # ChatService, RagService, KnowledgeService
│       └── controller/                 # REST API 控制器
│
├── docker/                             # Docker Compose 中间件编排
│   ├── docker-compose.yml              # PostgreSQL, Redis, RabbitMQ, MinIO (+ 可选 Ollama)
│   ├── docker-compose.app.yml          # 应用服务编排（可选）
│   └── .env.example                    # 环境变量模板
│
├── sql/                                # 数据库初始化脚本
│   └── init.sql                        # 建表语句 + pgvector 扩展
│
├── scripts/                            # 启动脚本
│   └── start.sh                        # 一键启动脚本
│
├── doc/                                # 文档
│   └── api-overview.md                 # API 接口概览
│
├── Dockerfile                          # 应用 Docker 镜像
├── Makefile                            # 常用命令快捷入口
└── pom.xml                             # 父 POM
```

## 快速开始（OpenAI）

### 环境要求

- Java 21+
- Docker & Docker Compose
- OpenAI API Key（或兼容接口的 Key：DeepSeek、通义千问、智谱 AI、Moonshot 等）

### 第一步：配置 API Key

项目根目录有 `.env` 文件，用于本地开发时加载环境变量。编辑它填入你的 API Key：

```bash
# 编辑项目根目录的 .env（不是 docker/.env）
# docker/.env 是给 Docker 容器用的，本地开发用根目录的 .env
```

`.env` 文件内容示例：

```env
OPENAI_API_KEY=sk-your-key-here
OPENAI_BASE_URL=https://api.openai.com
OPENAI_CHAT_MODEL=gpt-4o-mini
```

支持的 OpenAI 兼容接口（只需修改 `OPENAI_BASE_URL` 和 `OPENAI_CHAT_MODEL`）：

| 提供商 | Base URL | 对话模型示例 |
|--------|----------|-------------|
| OpenAI | https://api.openai.com | gpt-4o-mini |
| DeepSeek | https://api.deepseek.com | deepseek-chat |
| 通义千问 | https://dashscope.aliyuncs.com/compatible-mode | qwen-plus |
| 智谱 AI | https://open.bigmodel.cn/api/paas | glm-4-flash |
| Moonshot | https://api.moonshot.cn | moonshot-v1-8k |
| MiMo | https://token-plan-cn.xiaomimimo.com/v1 | mimo-v2.5-pro |

> **注意**：部分提供商（如 MiMo）可能不支持 Embedding 接口。此时 RAG 和知识库的向量检索功能不可用，但对话功能正常。系统已做兼容处理，不会因缺少 Embedding 而启动失败。

### 第二步：启动中间件

```bash
# 启动 PostgreSQL, Redis, RabbitMQ, MinIO
docker compose -f docker/docker-compose.yml up -d

# 查看服务状态
docker compose -f docker/docker-compose.yml ps
```

| 服务 | 端口 | 说明 |
|------|------|------|
| PostgreSQL + pgvector | 5432 | 向量数据库 + 业务数据库 |
| Redis 7 | 6379 | 缓存 + 对话记忆 |
| RabbitMQ 3.13 | 5672 / 15672 | 消息队列 + 管理控制台 |
| MinIO | 9000 / 9001 | 对象存储（文档管理） |

### 第三步：启动应用

**Linux / macOS：**

```bash
# 方式一：使用 make（自动加载 .env）
make run

# 方式二：手动运行（自动加载 .env）
set -a && source .env && set +a && ./mvnw spring-boot:run -pl spring-ai-template-app

# 方式三：先打包再运行（需手动 export 环境变量）
export OPENAI_API_KEY=sk-your-key-here
export OPENAI_BASE_URL=https://api.openai.com
./mvnw clean package -DskipTests
java -jar spring-ai-template-app/target/*.jar
```

**Windows PowerShell：**

```powershell
# 设置环境变量并启动
$env:OPENAI_API_KEY = "sk-your-key-here"
$env:OPENAI_BASE_URL = "https://api.openai.com"
$env:OPENAI_CHAT_MODEL = "gpt-4o-mini"
.\mvnw.cmd spring-boot:run -pl spring-ai-template-app
```

### 第四步：访问服务

| 服务 | 地址 | 说明 |
|------|------|------|
| API 基础路径 | http://localhost:8080/api | REST API |
| Knife4j | http://localhost:8080/doc.html | 增强版 API 文档 |
| Swagger UI | http://localhost:8080/swagger-ui.html | 交互式 API 文档 |
| RabbitMQ 控制台 | http://localhost:15672 | 用户名: admin / 密码: admin123 |
| MinIO 控制台 | http://localhost:9001 | 用户名: minioadmin / 密码: minioadmin |

## 可选：使用 Ollama 本地模型

如果你希望在本地运行模型，不调用云端 API：

```bash
# 1. 启动 Ollama 服务
docker compose -f docker/docker-compose.yml --profile ollama up -d

# 2. 拉取模型
docker exec ai-ollama ollama pull qwen2.5:7b
docker exec ai-ollama ollama pull nomic-embed-text

# 3. 使用 Ollama profile 启动应用（make 方式）
make run-ollama

# 或手动启动：
set -a && source .env && set +a && \
./mvnw spring-boot:run -pl spring-ai-template-app \
  -Dspring-boot.run.profiles=dev,ollama
```

## API 接口

### 对话接口

```bash
# 单轮对话
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "什么是 Spring AI？"}'

# 多轮对话（传入 conversationId 保持上下文）
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "请继续展开说明", "conversationId": "conv_xxx"}'

# 流式对话（SSE）
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "详细介绍一下 RAG 技术"}'
```

### RAG 检索增强生成

```bash
# 基于知识库的问答
curl -X POST http://localhost:8080/api/rag \
  -H "Content-Type: application/json" \
  -d '{"question": "这个项目有哪些主要功能？", "topK": 5, "similarityThreshold": 0.7}'

# 流式 RAG
curl -X POST http://localhost:8080/api/rag/stream \
  -H "Content-Type: application/json" \
  -d '{"question": "项目的架构设计是怎样的？"}'
```

### 知识库管理

```bash
# 创建知识库
curl -X POST "http://localhost:8080/api/knowledge?name=产品文档&description=产品相关文档"

# 查看所有知识库
curl http://localhost:8080/api/knowledge

# 查看知识库详情
curl http://localhost:8080/api/knowledge/{id}

# 上传文档到知识库
curl -X POST http://localhost:8080/api/knowledge/{id}/documents \
  -F "file=@document.pdf"

# 查看知识库下的文档列表
curl http://localhost:8080/api/knowledge/{id}/documents
```

## 配置说明

### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `OPENAI_API_KEY` | - | **必填。** OpenAI API Key |
| `OPENAI_BASE_URL` | api.openai.com | OpenAI 兼容接口地址 |
| `OPENAI_CHAT_MODEL` | gpt-4o-mini | 对话模型名称 |
| `OPENAI_EMBEDDING_MODEL` | text-embedding-3-small | 向量嵌入模型名称 |
| `POSTGRES_PASSWORD` | postgres | PostgreSQL 密码 |
| `REDIS_PASSWORD` | redis123 | Redis 密码 |
| `RABBITMQ_USER` | admin | RabbitMQ 用户名 |
| `RABBITMQ_PASSWORD` | admin123 | RabbitMQ 密码 |
| `OLLAMA_CHAT_MODEL` | qwen2.5:7b | Ollama 对话模型（可选） |
| `OLLAMA_EMBEDDING_MODEL` | nomic-embed-text | Ollama 向量嵌入模型（可选） |

### Spring Profiles

| Profile | 说明 |
|---------|------|
| `dev` | 开发环境（默认），使用 OpenAI |
| `prod` | 生产环境，所有配置通过环境变量注入 |
| `ollama` | 启用 Ollama 本地模型，需与 `dev` 或 `prod` 一起使用 |

```bash
# 使用 OpenAI（默认）
./mvnw spring-boot:run -pl spring-ai-template-app

# 使用 Ollama 本地模型
./mvnw spring-boot:run -pl spring-ai-template-app \
  -Dspring-boot.run.profiles=dev,ollama
```

## Docker 全量部署

```bash
# 构建应用镜像
docker build -t spring-ai-template:latest .

# 启动所有服务（中间件 + 应用）
docker compose -f docker/docker-compose.yml -f docker/docker-compose.app.yml --profile app up -d
```

## 常用命令

```bash
# 查看所有可用命令
make help

# 首次初始化（复制 .env + 拉取镜像）
make setup

# 启动中间件
make start

# 停止中间件
make stop

# 构建应用
make build

# 运行应用（OpenAI）
make run

# 查看中间件日志
make logs

# 清理所有数据（慎用）
make clean
```

## 架构说明

### 整体架构

```
┌─────────────────────────────────────────────────────┐
│                   客户端 (前端 / Postman)               │
└───────────────────────┬─────────────────────────────┘
                        │ HTTP / SSE
┌───────────────────────▼─────────────────────────────┐
│              Spring Boot Application                  │
│  ┌───────────┐  ┌───────────┐  ┌──────────────────┐ │
│  │ Chat API  │  │  RAG API  │  │ Knowledge API    │ │
│  └─────┬─────┘  └─────┬─────┘  └────────┬─────────┘ │
│        │              │                  │           │
│  ┌─────▼──────────────▼──────────────────▼─────────┐ │
│  │              Spring AI Framework                  │ │
│  │  ChatClient / EmbeddingModel / VectorStore       │ │
│  └─────┬──────────┬──────────┬──────────┬──────────┘ │
└────────┼──────────┼──────────┼──────────┼───────────┘
         │          │          │          │
   ┌─────▼───┐ ┌───▼────┐ ┌──▼───┐ ┌───▼────┐
   │ OpenAI  │ │  Redis │ │PGVec │ │RabbitMQ│
   │ (云端)  │ │ 缓存   │ │向量库│ │ 消息队列│
   └─────────┘ └────────┘ └──────┘ └────────┘
```

### RAG 流程

```
用户提问 → 向量嵌入 → PGVector 相似度检索 → 拼接上下文 → LLM 生成回答
     ↑
文档上传 → 文本解析 → 文本分块 → 向量嵌入 → 存入 PGVector
```

## 参考项目

本项目参考了以下优秀开源项目，在此表示感谢：

- [spring-projects/spring-ai](https://github.com/spring-projects/spring-ai) - Spring AI 官方框架
- [spring-projects/spring-ai-examples](https://github.com/spring-projects/spring-ai-examples) - Spring AI 官方示例
- [spring-ai-alibaba/examples](https://github.com/spring-ai-alibaba/examples) - Spring AI Alibaba 示例与最佳实践
- [AntonyCheng/spring-boot-init-template](https://github.com/AntonyCheng/spring-boot-init-template) - Spring Boot 模板架构设计
- [benayat/rag-with-spring-ai](https://github.com/benayat/rag-with-spring-ai) - RAG + Docker Compose 实现方案
- [habuma/spring-ai-examples](https://github.com/habuma/spring-ai-examples) - Craig Walls 的 Spring AI 示例
- [saranshbansal/spring-ai-template](https://github.com/saranshbansal/spring-ai-template) - Spring AI + PGVector 模板

## 开源协议

MIT
