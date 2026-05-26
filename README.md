# mem0-java

Intelligent memory layer for AI assistants — Java implementation.

mem0 enables LLM-based applications to persist, search, and manage contextual memories across conversations. This is a Java/Spring Boot port of the Python [mem0](https://github.com/mem0ai/mem0) project, achieving full feature parity with the V3 additive extraction pipeline.

## Features

### Core Memory Engine
- **V3 Additive Extraction Pipeline** — 8-phase batch pipeline with anti-hallucination UUID mapping, LLM extraction, batch embedding, hash dedup, entity linking
- **Hybrid Retrieval** — Semantic search + BM25 keyword search (with lemmatization) + entity boosts + additive score fusion + reranking
- **Procedural Memory** — Structured procedural memory extraction via dedicated system prompt
- **Entity Store** — Separate vector store collection for entity linking, upsert (0.95 threshold), spread-attenuated boost computation
- **Memory Deduplication** — SHA-256 hash-based deduplication
- **History Tracking** — Full audit trail of all memory changes (MySQL-backed)

### Providers
- **9 LLM Providers** — OpenAI, Anthropic, Azure OpenAI, Google Gemini, Ollama, DeepSeek, Groq, Together, vLLM
- **7 Embedding Providers** — OpenAI, Ollama, HuggingFace, Azure OpenAI, Google Gemini, Together, Vertex AI
- **8 Vector Stores** — PgVector, Qdrant, ChromaDB, Pinecone, Milvus, MongoDB, Redis, FAISS
- **2 Rerankers** — Cohere, LLM-based

### Infrastructure
- **Dual Database** — PostgreSQL (MyBatis + pgvector) for main data, MySQL (HikariCP) for history store
- **Advanced Metadata Filters** — eq, ne, gt, gte, lt, lte, in, nin, contains, icontains, AND, OR, NOT, wildcard
- **BM25 Scoring** — Query-length-adaptive sigmoid normalization with additive fusion
- **Rule-based Lemmatizer** — English lemmatization (200+ irregular verbs, suffix rules, -ing preservation)
- **Entity Extraction** — PROPER, QUOTED, ACRONYM, COMPOUND entity types with substring dedup
- **MemoryClient SDK** — REST client for mem0 platform API (add, get, search, update, delete, history, feedback, batch)
- **Authentication** — JWT + API key authentication with BCrypt password hashing
- **Rate Limiting** — Token bucket rate limiting per user/API key
- **Async Support** — CompletableFuture-based async operations

## Requirements

- Java 21+ (with preview features enabled)
- Maven 3.9+
- PostgreSQL 15+ with pgvector extension
- MySQL 8+ (for history store)
- Docker (optional, for integration tests)

## Quick Start

### 1. Build

```bash
./mvnw clean package -DskipTests
```

### 2. Configure Databases

Enable pgvector extension in PostgreSQL:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

Create MySQL database for history:

```sql
CREATE DATABASE mem0_history;
```

### 3. Configure Application

Edit `mem0/mem0-server/src/main/resources/application.yml` or set environment variables:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mem0
    username: mem0
    password: mem0

mem0:
  embedding:
    provider: openai
    config:
      api-key: ${OPENAI_API_KEY}
      model: text-embedding-3-small
  llm:
    provider: openai
    config:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o-mini
  vector-store:
    provider: pgvector
    config:
      collection-name: memories
  history:
    mysql:
      url: jdbc:mysql://localhost:3306/mem0_history?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      username: mem0
      password: mem0
```

### 4. Run

```bash
./mvnw spring-boot:run -pl mem0/mem0-server
```

Or with Docker:

```bash
docker build -t mem0-java .
docker run -p 8080:8080 \
  -e OPENAI_API_KEY=sk-... \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/mem0 \
  mem0-java
```

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Login and get JWT token |
| POST | `/auth/refresh` | Refresh JWT token |

### Memories

| Method | Endpoint | Description                              |
|--------|----------|------------------------------------------|
| POST | `/memories` | Add memories from messages (V3 pipeline) |
| GET | `/memories` | List memories (with filters)             |
| GET | `/memories/{id}` | Get a specific memory                    |
| PUT | `/memories/{id}` | Update a memory                          |
| DELETE | `/memories/{id}` | Delete a memory                          |
| GET | `/memories/{id}/history` | Get memory change history                |
| POST | `/memories/search` | Search memories (hybrid retrieval)       |
| DELETE | `/memories` | Delete all memories (with filters)       |
| POST | `/memories/reset` | Reset all memories                       |

### API Keys

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api-keys` | Create a new API key |
| GET | `/api-keys` | List API keys |
| DELETE | `/api-keys/{id}` | Revoke an API key |

### Configuration

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/configure` | Get current configuration |
| POST | `/configure` | Update configuration |
| GET | `/configure/providers` | List available providers |

### Other

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/entities` | List entities (users, agents, runs) |
| DELETE | `/entities/{type}/{id}` | Delete entity and cascade |
| GET | `/requests` | List request logs |
| GET | `/actuator/health` | Health check |

### Swagger UI

Access interactive API documentation at: `http://localhost:8080/swagger-ui.html`

## Configuration Reference

### LLM Providers

| Provider | Config | Default Model |
|----------|--------|---------------|
| openai | `mem0.llm.provider=openai` | gpt-4o-mini |
| anthropic | `mem0.llm.provider=anthropic` | claude-3-5-sonnet-20241022 |
| azure | `mem0.llm.provider=azure` | (deployment name) |
| gemini | `mem0.llm.provider=gemini` | gemini-1.5-flash |
| ollama | `mem0.llm.provider=ollama` | llama3 |
| deepseek | `mem0.llm.provider=deepseek` | deepseek-chat |
| groq | `mem0.llm.provider=groq` | llama-3.1-70b-versatile |
| together | `mem0.llm.provider=together` | meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo |
| vllm | `mem0.llm.provider=vllm` | (configured model) |

### Embedding Providers

| Provider | Config | Default Model | Dimension |
|----------|--------|---------------|-----------|
| openai | `mem0.embedding.provider=openai` | text-embedding-3-small | 1536 |
| ollama | `mem0.embedding.provider=ollama` | nomic-embed-text | 768 |
| huggingface | `mem0.embedding.provider=huggingface` | BAAI/bge-small-en-v1.5 | 384 |
| azure | `mem0.embedding.provider=azure` | (deployment name) | 1536 |
| gemini | `mem0.embedding.provider=gemini` | text-embedding-004 | 768 |
| together | `mem0.embedding.provider=together` | togethercomputer/m2-bert-80M-8k-retrieval | 768 |
| vertexai | `mem0.embedding.provider=vertexai` | text-embedding-004 | 768 |

### Vector Store Providers

| Provider | Config | Notes |
|----------|--------|-------|
| pgvector | `mem0.vectorstore.provider=pgvector` | Default, uses PostgreSQL |
| qdrant | `mem0.vectorstore.provider=qdrant` | REST API, requires Qdrant server |
| chroma | `mem0.vectorstore.provider=chroma` | REST API, requires ChromaDB server |
| pinecone | `mem0.vectorstore.provider=pinecone` | REST API, requires API key |
| milvus | `mem0.vectorstore.provider=milvus` | REST API v2 |
| faiss | `mem0.vectorstore.provider=faiss` | Local file-based, no server needed |
| mongodb | `mem0.vectorstore.provider=mongodb` | Requires MongoDB Atlas Vector Search |
| redis | `mem0.vectorstore.provider=redis` | Requires Redis Stack |

## Architecture

### V3 Additive Extraction Pipeline

The `add()` method implements an 8-phase batch pipeline ported from Python mem0:

```
Phase 0: Context gathering (messages input)
Phase 1: Existing memory retrieval + UUID→Int mapping (anti-hallucination)
Phase 2: LLM extraction (ADDITIVE_EXTRACTION_PROMPT), fallback to FactExtractor
Phase 3: Batch embedding (embedBatch, fallback to individual)
Phase 4: Per-memory processing + SHA-256 hash dedup + BM25 lemmatization
Phase 5: Batch persist to VectorStore
Phase 6: Batch entity linking (EntityStore.linkEntitiesForMemory)
Phase 7: Return results
```

### Hybrid Retrieval

The `search()` method combines multiple signals:

```
1. Query lemmatization (Lemmatizer)
2. Semantic search (over-fetch: max(topK*4, 60))
3. Keyword search (keywordSearch, optional)
4. Entity boost computation (EntityStore.computeEntityBoosts)
5. Additive score fusion (Scoring.scoreAndRank)
6. Reranking (Reranker, optional)
```

Score fusion formula: `combined = (semantic + bm25 + entityBoost) / maxPossible`

### Project Structure

```
mem0-java/
├── mem0/                         # Java backend (Maven multi-module)
│   ├── pom.xml                   # Parent POM
│   ├── mem0-core/                # Core library
│   │   └── src/main/java/cn/hsine/mem0/core/
│   │       ├── client/           # MemoryClient REST SDK
│   │       ├── config/           # Prompts, PromptBuilder, MemoryConfig, MemoryType
│   │       ├── domain/model/     # Entities (Memory, MemoryHistory, MemoryEvent)
│   │       ├── dto/              # Request/Response DTOs
│   │       ├── embedding/        # Embedding provider interface + 7 implementations
│   │       ├── entityextractor/  # Entity extraction interface + LLM implementation
│   │       ├── entitystore/      # Entity store interface + PgEntityStore
│   │       ├── exception/        # Exception types with error codes
│   │       ├── llm/              # LLM provider interface + 9 implementations
│   │       ├── message/          # Message entity and service
│   │       ├── reranker/         # Reranker interface + 6 implementations
│   │       ├── repository/       # MyBatis repositories + type handlers
│   │       ├── score/            # BM25 scoring parameters
│   │       ├── service/          # MemoryService, TelemetryService
│   │       ├── utils/            # Scoring, Lemmatizer, EntityExtractorUtil, MetadataFilter
│   │       └── vectorstore/      # VectorStore interface + 8 implementations
│   ├── mem0-core/src/main/resources/
│   │   ├── db/migration/         # Flyway migrations (V1-V3)
│   │   └── mapper/              # MyBatis XML mappers
│   └── mem0-server/              # REST API server
│       └── src/main/java/cn/hsine/mem0/server/
│           ├── config/           # OpenAPI configuration
│           ├── controller/       # REST controllers
│           ├── domain/model/     # Server entities (User, ApiKey, RequestLog)
│           ├── dto/              # Request/Response DTOs
│           ├── exception/        # Global exception handler
│           ├── health/           # Health indicators
│           ├── repository/       # MyBatis repositories
│           ├── security/         # JWT, API key, rate limiting, request logging
│           └── service/          # Auth, ApiKey, Config, Entity services
├── dashboard/                    # Web dashboard (Next.js)
│   └── src/
│       ├── app/                  # App Router pages
│       │   ├── (auth)/           # Login page
│       │   ├── (root)/           # Main app (dashboard, memories, analytics, etc.)
│       │   ├── api/              # API route handlers
│       │   └── setup/            # Setup wizard
│       ├── components/           # UI components (shadcn/ui + custom)
│       ├── hooks/                # React hooks
│       ├── lib/                  # Utilities and validators
│       ├── store/                # Redux store
│       ├── styles/               # Global styles and animations
│       ├── types/                # TypeScript type definitions
│       └── utils/                # API client and helpers
├── Dockerfile                    # Backend Docker build
└── .github/workflows/ci.yml     # CI pipeline
```

## Testing

```bash
# Unit tests only
./mvnw test -pl mem0/mem0-core

# All tests (requires Docker for Testcontainers)
./mvnw verify

# Skip tests
./mvnw package -DskipTests
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `OPENAI_API_KEY` | — | OpenAI API key (for LLM + embeddings) |
| `LLM_PROVIDER` | openai | LLM provider name |
| `LLM_MODEL` | gpt-4o-mini | LLM model name |
| `EMBEDDING_PROVIDER` | openai | Embedding provider name |
| `EMBEDDING_MODEL` | text-embedding-3-small | Embedding model name |
| `VECTOR_STORE_PROVIDER` | pgvector | Vector store provider |
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | mem0 | PostgreSQL database |
| `DB_USER` | mem0 | PostgreSQL user |
| `DB_PASSWORD` | mem0 | PostgreSQL password |
| `HISTORY_DB_HOST` | localhost | MySQL host (history store) |
| `HISTORY_DB_PORT` | 3306 | MySQL port |
| `HISTORY_DB_NAME` | mem0_history | MySQL database |
| `HISTORY_DB_USER` | mem0 | MySQL user |
| `HISTORY_DB_PASSWORD` | mem0 | MySQL password |
| `JWT_SECRET` | (change in prod) | JWT signing key |
| `SERVER_PORT` | 8080 | HTTP server port |

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 (preview features) |
| Framework | Spring Boot 4.1.0-RC1 |
| ORM | MyBatis 4.0.1 |
| Primary DB | PostgreSQL 15+ with pgvector |
| History DB | MySQL 8+ (HikariCP) |
| Migrations | Flyway |
| JSON | Jackson |
| Auth | JWT + BCrypt |
| Build | Maven |
| CI | GitHub Actions |
| Container | Docker (Eclipse Temurin 21) |
| Dashboard | Next.js 15 / React 19 / TypeScript / Tailwind CSS |

## License

Apache 2.0
