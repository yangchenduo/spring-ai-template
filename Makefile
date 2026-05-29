# ============================================================
#  Spring AI Template - Makefile
# ============================================================

.PHONY: help setup start stop restart build run logs clean

COMPOSE = docker compose -f docker/docker-compose.yml
MVN = ./mvnw

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

setup: ## First-time setup: copy .env, pull images, init DB
	@cp -n docker/.env.example docker/.env 2>/dev/null || true
	@echo "Pulling Docker images..."
	$(COMPOSE) pull
	@echo "Setup complete. Run 'make start' to launch middleware."

start: ## Start all middleware services
	$(COMPOSE) up -d
	@echo "Waiting for services to be healthy..."
	@sleep 10
	$(COMPOSE) ps

stop: ## Stop all middleware services
	$(COMPOSE) down

restart: stop start ## Restart all middleware services

build: ## Build the application
	$(MVN) clean package -DskipTests

run: ## Run the application (loads .env, dev profile)
	@set -a && source .env 2>/dev/null && set +a && \
	$(MVN) spring-boot:run -pl spring-ai-template-app

run-ollama: ## Run with Ollama profile (loads .env)
	@set -a && source .env 2>/dev/null && set +a && \
	$(MVN) spring-boot:run -pl spring-ai-template-app \
		-Dspring-boot.run.profiles=dev,ollama

ollama-pull: ## Pull default Ollama models
	docker exec ai-ollama ollama pull qwen2.5:7b
	docker exec ai-ollama ollama pull nomic-embed-text

logs: ## Show middleware logs
	$(COMPOSE) logs -f

logs-app: ## Show application logs
	$(COMPOSE) logs -f app

clean: ## Remove all containers and volumes (DESTRUCTIVE)
	$(COMPOSE) down -v
	@echo "All data volumes removed."

status: ## Show middleware status
	$(COMPOSE) ps

test: ## Run tests
	$(MVN) test

docker-build: ## Build application Docker image
	docker build -t spring-ai-template:latest .

docker-up: ## Start everything (middleware + app)
	$(COMPOSE) --profile app up -d
