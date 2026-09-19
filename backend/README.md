# AI Study Companion — Backend

Spring Boot 21 backend for an AI-powered learning workspace. Core loop:
**Space → Project → PDF Material → async processing → RAG Tutor → Adaptive Quiz →
Assessment → Concept Mastery → Growth → Recommendations.**

**Context first:** every AI call is scoped to the selected project.
**Evidence over guessing:** the tutor answers from project material and says so
explicitly when the material doesn't cover the question.

## Architecture

```
Controller → Service → Repository / external service
```

| Package | Responsibility |
|---|---|
| `auth`, `user` | Register/login/me, BCrypt, JWT (`JwtAuthFilter`, `SecurityConfig`) |
| `space`, `project` | CRUD with service-level ownership (`findByIdAndUserId`) |
| `material` | PDF upload → `QUEUED`; `MaterialProcessor` (`@Async`) → extract (PDFBox) → chunk → embed → `READY`/`FAILED` with attempts + retry |
| `knowledge` | `DocumentChunk` (pgvector), `EmbeddingService`, project-scoped `RetrievalService`, `Concept` extraction |
| `ai` | `AiClient` interface — **all** AI calls go through it; `OpenAiClient` (live API or offline heuristic fallback); `AiResponseValidator` |
| `tutor` | Conversations/messages, grounded chat with citations |
| `quiz` | Generation, `AdaptiveQuizService` (weakest mastery + mistakes first), `AssessmentService` (MCQ + open-ended `AssessmentResult`) |
| `learning` | `ConceptMastery` (EMA estimate), `GrowthService` (IMPROVING/STABLE/REQUIRING_ATTENTION), recommendations, `LearningContext` digest |
| `analytics` | Idempotent `ActivityEvent`s, `AiUsage` per call, project/global stats |
| `admin` | `ROLE_ADMIN` APIs: users, activity, AI usage, processing failures |
| `common` | `ApiError` + `@RestControllerAdvice`, health, OpenAPI config |

## Setup

Requirements: Java 21, Maven, PostgreSQL 16 + pgvector 0.6 on port 5433.

```bash
# 1. Database (already provisioned for this project)
PGPASSWORD=aistudy_dev psql -h localhost -p 5433 -U aistudy -d ai_study \
  -c "CREATE EXTENSION IF NOT EXISTS vector;"

# 2. Configure (optional — defaults in application.properties already match)
cp .env.example .env   # then edit values

# 3. Run (Flyway migrates automatically, ddl-auto=validate)
./mvnw spring-boot:run

# 4. Tests
./mvnw clean test
```

With `OPENROUTER_API_KEY` unset, the backend runs fully offline with deterministic
heuristics (labelled `offline-heuristic` in AI usage). Set the key (export
OPENROUTER_API_KEY=... before starting the backend) to use the live OpenRouter
tutor/quiz path + `text-embedding-3-small`.

## API overview

Auth: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`
(all other endpoints need `Authorization: Bearer <token>`; `/api/health` is public).

- Spaces: `GET/POST /api/spaces`, `GET/PUT/DELETE /api/spaces/{id}`
- Projects: `GET/POST /api/spaces/{spaceId}/projects`, `GET/PUT/DELETE /api/projects/{projectId}`
- Materials: `GET /api/projects/{pid}/materials`, `POST .../materials` (multipart `file`, PDF only),
  `GET .../materials/{mid}`, `POST .../materials/{mid}/retry`
- Concepts: `GET /api/projects/{pid}/concepts`
- Tutor: `POST /api/projects/{pid}/tutor/chat` → `{answer, grounded, citations[]}`,
  `GET .../tutor/conversations`, `GET /api/conversations/{cid}/messages`
- Quiz: `POST /api/projects/{pid}/quizzes`, `GET /api/quizzes/{qid}`,
  `POST /api/quizzes/{qid}/questions/{questionId}/answers`
  (alias `POST /api/quizzes/{qid}/answers` with `questionId` in body),
  `POST /api/quizzes/{qid}/complete`
- Learning: `GET /api/projects/{pid}/mastery|growth|context`,
  `GET/POST /api/projects/{pid}/recommendations`
- Analytics: `GET /api/projects/{pid}/analytics`, `GET /api/analytics/overview`
- Admin (`ADMIN` role): `/api/admin/users|spaces|projects|activity|analytics|ai-usage|material-processing|health`
- Docs: `/swagger-ui.html`, `/api-docs`

## Quick manual test

```bash
B=http://localhost:8080
TOKEN=$(curl -s -X POST $B/api/auth/register -H 'Content-Type: application/json' \
  -d '{"name":"Demo","email":"demo@test.com","password":"password123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
SID=$(curl -s -X POST $B/api/spaces -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"ML"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['id'])")
PID=$(curl -s -X POST $B/api/spaces/$SID/projects -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Gradients","goal":"master optimization"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['id'])")
curl -X POST $B/api/projects/$PID/materials -H "Authorization: Bearer $TOKEN" -F "file=@notes.pdf;type=application/pdf"
# poll until READY, then:
curl -X POST $B/api/projects/$PID/tutor/chat -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"message":"What is gradient descent?"}'
```

## RAG architecture

Chunk (900 chars, 120 overlap, page numbers kept) → embed (OpenAI or hashing
fallback, 1536-d, L2-normalized) → `vector(1536)` column with HNSW cosine index →
`ORDER BY embedding <=> :queryVector` **always filtered by `(project_id, user_id)`**.
Keyword fallback uses the same project scope. Citations are validated against
retrieved material — hallucinated sources are dropped and `grounded` forced false.

## Security / data isolation

JWT + BCrypt; `ROLE_USER`/`ROLE_ADMIN`; every repository lookup is ownership-scoped
(`findByIdAndUserId`); RAG never crosses projects; PDFs/user text are untrusted
data (injection instructions in documents are ignored by the tutor system prompt);
consistent `ApiError` responses, no stack traces; secrets via env vars only.

## Async processing

Upload commits, then `afterCommit` fires `MaterialProcessor.processAsync` on a
dedicated executor: PROCESSING → PDFBox extract → chunk → embed → concepts → READY.
Idempotency guard skips PROCESSING/READY rows; FAILED keeps `processingAttempts`
(max 3) + `errorMessage`; `POST .../retry` re-queues.

## Production deployment

Requirements: Java 21, PostgreSQL 16 + pgvector 0.6, Node 18+ (frontend).

1. Database: create DB/user, enable `vector` extension, Flyway migrates on boot
   (`ddl-auto=validate` — schema is owned by `V1__init.sql`).
2. Configure via environment (see `.env.example` — names only, no secrets):
   `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `JWT_SECRET` (long random string,
   required), `CORS_ALLOWED_ORIGINS` (deployed frontend origin, comma-separated),
   `OPENROUTER_API_KEY/BASE_URL/CHAT_MODEL`, `STORAGE_LOCATION`, `PORT`.
3. Build: `./mvnw clean package` → `java -jar target/*.jar`.
4. Frontend: `VITE_API_BASE_URL=<backend-url>`, `VITE_USE_MOCKS=false`,
   then `npm install && npm run build` and serve `dist/` (SPA fallback to
   `index.html` required for deep links/refresh).
5. Uploads: PDF-only, max 100 MB per file (`MaterialService` enforces;
   container multipart cap 100/105 MB).

Backup before any destructive DB operation (never `DROP`/`TRUNCATE` prod):

```bash
PGPASSWORD=<pw> pg_dump -h <host> -p 5433 -U <user> -Fc ai_study > ai_study_$(date +%F).dump
```

Known limitations: see above (offline heuristic, embedding mix, EMA mastery,
local storage, no refresh tokens).

## Next steps

1. Set `OPENROUTER_API_KEY` and tune prompts/top-K.
2. S3-backed `StorageService`, refresh-token rotation.
3. Denser tests around `MaterialProcessor` with Testcontainers Postgres+pgvector.
4. Frontend wiring (endpoints match the spec's React client).
5. Mastery history charts, spaced-repetition scheduling, quiz streaming.
