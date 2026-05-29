#!/bin/bash
# ============================================================
#  Spring AI Template - Quick Start Script
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "=========================================="
echo "  Spring AI Template - Quick Start"
echo "=========================================="

# Step 1: Check Docker
echo ""
echo "[1/5] Checking Docker..."
if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed. Please install Docker first."
    exit 1
fi
docker --version

# Step 2: Setup .env
echo ""
echo "[2/5] Setting up environment..."
cd "$PROJECT_DIR/docker"
if [ ! -f .env ]; then
    cp .env.example .env
    echo "Created .env from .env.example"
else
    echo ".env already exists, skipping."
fi

# Step 3: Start middleware
echo ""
echo "[3/5] Starting middleware services..."
docker compose up -d
echo "Waiting for services to be healthy..."
sleep 15

# Step 4: Pull Ollama models
echo ""
echo "[4/5] Pulling Ollama models (this may take a few minutes)..."
docker exec ai-ollama ollama pull qwen2.5:7b || echo "Warning: Failed to pull qwen2.5:7b"
docker exec ai-ollama ollama pull nomic-embed-text || echo "Warning: Failed to pull nomic-embed-text"

# Step 5: Build and run
echo ""
echo "[5/5] Building and starting the application..."
cd "$PROJECT_DIR"
./mvnw clean package -DskipTests
./mvnw spring-boot:run -pl spring-ai-template-app

echo ""
echo "=========================================="
echo "  Application started!"
echo "  API:       http://localhost:8080/api"
echo "  Swagger:   http://localhost:8080/swagger-ui.html"
echo "  RabbitMQ:  http://localhost:15672"
echo "  MinIO:     http://localhost:9001"
echo "=========================================="
