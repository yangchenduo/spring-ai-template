# API Overview

## Base URL
```
http://localhost:8080/api
```

## Endpoints

### Chat
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/chat | Single-turn chat |
| POST | /api/chat/stream | Streaming chat (SSE) |

### RAG
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/rag | Knowledge-based QA |
| POST | /api/rag/stream | Streaming RAG (SSE) |

### Knowledge Base
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/knowledge | Create knowledge base |
| GET | /api/knowledge | List knowledge bases |
| GET | /api/knowledge/{id} | Get knowledge base |
| DELETE | /api/knowledge/{id} | Delete knowledge base |
| POST | /api/knowledge/{id}/documents | Upload document |
| GET | /api/knowledge/{id}/documents | List documents |

### Health
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/health | Health check |

## Request/Response Examples

### Chat Request
```json
{
  "message": "What is Spring AI?",
  "conversationId": null,
  "provider": "openai",
  "model": null,
  "temperature": 0.7
}
```

### Chat Response
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "conversationId": "conv_abc123",
    "message": "Spring AI is...",
    "provider": "openai",
    "model": "gpt-4o-mini",
    "elapsed": 1234
  }
}
```

### RAG Request
```json
{
  "question": "What are the main features of this project?",
  "knowledgeIds": ["default"],
  "topK": 5,
  "similarityThreshold": 0.7,
  "provider": "openai"
}
```
