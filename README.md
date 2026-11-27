# Spring AI Template

[中文文档](README_CN.md)

A production-ready Spring Boot + Spring AI template project with Docker-managed middleware.

## Features

- **Multi-provider AI Chat** - OpenAI (default) + Ollama (optional local), with streaming support
- **RAG (Retrieval-Augmented Generation)** - Document-based QA with PGVector
- **Knowledge Base Management** - Upload, parse, and vectorize documents
- **Chat Memory** - Per-conversation context window, persisted to PostgreSQL
- **Async Document Processing** - RabbitMQ-driven document ingestion pipeline
- **API Documentation** - SpringDoc / Swagger UI (OpenAPI 3)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 4.2.0, Spring AI 2.1.0 |
| Runtime | Java 21 (`JAVA_HOME` must point to a JDK 21 install) |
| LLM | OpenAI / compatible (default), Ollama (optional) |
| Vector Store | PostgreSQL + pgvector |
| Cache / Session | Redis 7 |
| Message Queue | RabbitMQ 3.13 |
| ORM | MyBatis-Plus 3.5.18.1 |
| Object Storage | MinIO |
| API Docs | SpringDoc 3.2.1 / Swagger UI |
| Container | Docker Compose |

## Project Structure

```
spring-ai-template/
├── spring-ai-template-common/     # Shared DTOs, enums, utils
│   └── src/main/java/com/template/common/
│       ├── dto/                   # ChatRequest, RagRequest, etc.
│       ├── enums/                 # AIProvider, ErrorCode
│       ├── result/                # Result, PageResult
│       └── utils/                 # GlobalExceptionHandler, etc.
│
├── spring-ai-template-app/        # Main application
│   └── src/main/java/com/template/app/
│       ├── config/                # AI, Redis, RabbitMQ, Web configs
│       ├── entity/                # DB entities
│       ├── mapper/                # MyBatis-Plus mappers
│       ├── service/               # Business logic
│       │   └── impl/              # ChatService, RagService, KnowledgeService
│       └── controller/            # REST APIs
│
├── docker/                        # Docker Compose for middleware
│   ├── docker-compose.yml         # PostgreSQL, Redis, RabbitMQ, MinIO (+ Ollama optional)
│   ├── docker-compose.app.yml     # Application service (optional)
│   └── .env.example               # Environment variables template
│
├── sql/                           # Database initialization scripts
│   └── init.sql                   # Schema + pgvector extension
│
├── scripts/                       # Startup scripts
│   └── start.sh                   # One-click setup script
│
├── Dockerfile                     # Application Docker image
├── Makefile                       # Common commands
└── pom.xml                        # Parent POM
```

## Quick Start (OpenAI)

### Prerequisites

- Java 21+ (Maven takes the JDK from `JAVA_HOME`, so point it at the JDK 21 install even if an older JDK is on `PATH`)
- Docker & Docker Compose
- An OpenAI API key (or compatible provider: DeepSeek, Qwen, ZhipuAI, Moonshot, etc.)

### Step 1: Configure API Key

Edit the `.env` file in the project root (not `docker/.env`, which is for Docker containers). The application imports it automatically through `spring.config.import`, so no shell export is needed (real environment variables still take precedence):

```bash
cp .env.example .env      # then fill in the two keys
```

```env
OPENAI_API_KEY=sk-your-key-here          # chat endpoint
OPENAI_BASE_URL=https://api.openai.com
OPENAI_CHAT_MODEL=gpt-4o-mini

EMBEDDING_BASE_URL=https://api.siliconflow.cn/v1   # embedding endpoint
EMBEDDING_API_KEY=sk-your-key-here
EMBEDDING_MODEL=BAAI/bge-m3
EMBEDDING_DIMENSIONS=1024
```

Supported OpenAI-compatible providers:

| Provider | Base URL | Chat Model Example |
|----------|----------|-------------------|
| OpenAI | https://api.openai.com | gpt-4o-mini |
| DeepSeek | https://api.deepseek.com | deepseek-chat |
| Qwen | https://dashscope.aliyuncs.com/compatible-mode | qwen-plus |
| ZhipuAI | https://open.bigmodel.cn/api/paas | glm-4-flash |
| Moonshot | https://api.moonshot.cn | moonshot-v1-8k |
| MiMo | https://token-plan-cn.xiaomimimo.com/v1 | mimo-v2.5-pro |

> **Note**: Some providers (e.g. MiMo) may not support Embedding API. RAG/knowledge-base vector search won't work, but chat is fully functional. The system handles this gracefully.

### Step 2: Start Middleware

```bash
# Start PostgreSQL, Redis, RabbitMQ, MinIO
docker compose -f docker/docker-compose.yml up -d

# Check services status
docker compose -f docker/docker-compose.yml ps
```

| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL + pgvector | 5432 | Vector store + business DB |
| Redis | 6379 | Cache + Sa-Token session store |
| RabbitMQ | 5672 / 15672 | Message queue + management UI |
| MinIO | 9000 / 9001 | Object storage |

### Step 3: Run the Application

**Linux / macOS:**

```bash
# Option 1: Using make (auto-loads .env)
make run

# Option 2: Manual (auto-loads .env)
set -a && source .env && set +a && ./mvnw spring-boot:run -pl spring-ai-template-app

# Option 3: Build JAR first (export env vars manually)
export OPENAI_API_KEY=sk-your-key-here
export OPENAI_BASE_URL=https://api.openai.com
./mvnw clean package -DskipTests
java -jar spring-ai-template-app/target/*.jar
```

**Windows PowerShell:**

```powershell
# Set environment variables and run
$env:OPENAI_API_KEY = "sk-your-key-here"
$env:OPENAI_BASE_URL = "https://api.openai.com"
$env:OPENAI_CHAT_MODEL = "gpt-4o-mini"
.\mvnw.cmd spring-boot:run -pl spring-ai-template-app
```

### Step 4: Access

- **API Base**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **RabbitMQ Console**: http://localhost:15672 (admin/admin123)
- **MinIO Console**: http://localhost:9001 (minioadmin/minioadmin)

## Optional: Keep Chat / Embeddings On-Prem

Chat and embedding are two independent OpenAI-compatible clients, so going on-prem is a URL
change - no profile, no code, no image rebuild:

| What | Variables to change |
|------|---------------------|
| Chat | `OPENAI_BASE_URL` (+ `OPENAI_CHAT_MODEL`) |
| Embedding | `EMBEDDING_BASE_URL` (+ `EMBEDDING_MODEL`, `EMBEDDING_DIMENSIONS`) |

```bash
# 1. Start the optional local Ollama container
docker compose -f docker/docker-compose.yml --profile ollama up -d

# 2. Pull models (bge-m3 is 1024-dim - same width as the cloud default)
docker exec ai-ollama ollama pull bge-m3
docker exec ai-ollama ollama pull qwen2.5:7b

# 3. Point the embedding endpoint at it and run the app as usual
#    (use http://ollama:11434/v1 when the app runs inside a container)
EMBEDDING_BASE_URL=http://localhost:11434/v1
EMBEDDING_MODEL=bge-m3
EMBEDDING_API_KEY=ollama        # Ollama ignores the key, but it must be non-empty
./mvnw spring-boot:run -pl spring-ai-template-app
```

vLLM, LocalAI, TEI gateways and any other server exposing `/v1/embeddings` work the same way.

> Staying on 1024 dimensions keeps the existing `vector_store` table usable. A model with a
> different dimension requires `DROP TABLE vector_store`, a restart (prod DDL lives in
> `sql/init.sql`) and re-ingesting every document through the reprocess endpoint.

## API Endpoints

### Chat API

```bash
# Single-turn chat
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is Spring AI?"}'

# Multi-turn chat
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Tell me more", "conversationId": "conv_xxx"}'

# Streaming chat (SSE)
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "Explain RAG in detail"}'
```

### RAG API

```bash
# Knowledge-based QA
curl -X POST http://localhost:8080/api/rag \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the main features?", "topK": 5, "similarityThreshold": 0.7}'
```

### Knowledge Base API

```bash
# Create knowledge base
curl -X POST "http://localhost:8080/api/knowledge?name=MyKB&description=Test"

# List knowledge bases
curl http://localhost:8080/api/knowledge

# Upload document
curl -X POST http://localhost:8080/api/knowledge/{id}/documents \
  -F "file=@document.pdf"

# List documents
curl http://localhost:8080/api/knowledge/{id}/documents
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `OPENAI_API_KEY` | - | **Required.** OpenAI API key |
| `OPENAI_BASE_URL` | api.openai.com | OpenAI-compatible base URL |
| `OPENAI_CHAT_MODEL` | gpt-4o-mini | Chat model name |
| `EMBEDDING_BASE_URL` | api.siliconflow.cn/v1 | Embedding endpoint (any OpenAI-compatible `/v1`, cloud or on-prem) |
| `EMBEDDING_API_KEY` | - | **Required.** Key for the embedding endpoint |
| `EMBEDDING_MODEL` | BAAI/bge-m3 | Embedding model (1024 dims by default) |
| `EMBEDDING_DIMENSIONS` | 1024 | pgvector column width, must match the model |
| `STORAGE_TYPE` | local | Raw file storage (`local`; implement `DocumentStorage` for MinIO/S3) |
| `STORAGE_ROOT` | ./data/documents | Local storage root (`/app/data/documents` in the container) |
| `POSTGRES_PASSWORD` | postgres | PostgreSQL password |
| `REDIS_PASSWORD` | redis123 | Redis password |
| `RABBITMQ_USER` | admin | RabbitMQ username |
| `RABBITMQ_PASSWORD` | admin123 | RabbitMQ password |
| `OLLAMA_CHAT_MODEL` | qwen2.5:7b | Ollama chat model (optional, on-prem experiments) |

## Docker Full Deployment

```bash
# Build application image
docker build -t spring-ai-template:latest .

# Run all services (middleware + app)
docker compose -f docker/docker-compose.yml -f docker/docker-compose.app.yml --profile app up -d
```

Uploaded documents are written to the named volume `app_documents` mounted at
`/app/data/documents`, so they survive container recreation.

## Development

```bash
# Run tests
./mvnw test

# Check dependencies
./mvnw dependency:tree
```

## References

This project was inspired by and references the following open-source projects:

- [spring-projects/spring-ai](https://github.com/spring-projects/spring-ai) - Official Spring AI framework
- [spring-projects/spring-ai-examples](https://github.com/spring-projects/spring-ai-examples) - Official Spring AI examples
- [spring-ai-alibaba/examples](https://github.com/spring-ai-alibaba/examples) - Spring AI Alibaba examples and best practices
- [AntonyCheng/spring-boot-init-template](https://github.com/AntonyCheng/spring-boot-init-template) - Spring Boot template architecture design
- [benayat/rag-with-spring-ai](https://github.com/benayat/rag-with-spring-ai) - RAG implementation with Docker Compose
- [habuma/spring-ai-examples](https://github.com/habuma/spring-ai-examples) - Spring AI usage examples by Craig Walls
- [saranshbansal/spring-ai-template](https://github.com/saranshbansal/spring-ai-template) - Spring AI template with PGVector

## License

MIT
