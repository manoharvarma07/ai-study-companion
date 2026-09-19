CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS vector;

-- ============ users ============
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

-- ============ spaces ============
CREATE TABLE IF NOT EXISTS spaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_spaces_user_id ON spaces (user_id);

-- ============ projects ============
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    space_id UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    goal TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_projects_user_id ON projects (user_id);
CREATE INDEX IF NOT EXISTS idx_projects_space_id ON projects (space_id);

-- ============ materials ============
CREATE TABLE IF NOT EXISTS materials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    filename VARCHAR(500) NOT NULL,
    content_type VARCHAR(150),
    file_size BIGINT NOT NULL DEFAULT 0,
    storage_path VARCHAR(1000),
    page_count INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    processing_attempts INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_materials_user_id ON materials (user_id);
CREATE INDEX IF NOT EXISTS idx_materials_project_id ON materials (project_id);
CREATE INDEX IF NOT EXISTS idx_materials_status ON materials (status);

-- ============ document_chunks (pgvector) ============
CREATE TABLE IF NOT EXISTS document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_id UUID NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    page_number INTEGER,
    chunk_index INTEGER NOT NULL DEFAULT 0,
    embedding vector(1536),
    chunk_metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_chunks_material_id ON document_chunks (material_id);
CREATE INDEX IF NOT EXISTS idx_chunks_project_id ON document_chunks (project_id);
CREATE INDEX IF NOT EXISTS idx_chunks_user_id ON document_chunks (user_id);
CREATE INDEX IF NOT EXISTS idx_chunks_created_at ON document_chunks (created_at);
-- HNSW index for cosine similarity (requires pgvector >= 0.6)
CREATE INDEX IF NOT EXISTS idx_chunks_embedding_hnsw
    ON document_chunks USING hnsw (embedding vector_cosine_ops);

-- ============ concepts ============
CREATE TABLE IF NOT EXISTS concepts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(300) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_concepts_project_name UNIQUE (project_id, name)
);
CREATE INDEX IF NOT EXISTS idx_concepts_project_id ON concepts (project_id);
CREATE INDEX IF NOT EXISTS idx_concepts_user_id ON concepts (user_id);

-- ============ conversations / messages ============
CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_conversations_project_id ON conversations (project_id);
CREATE INDEX IF NOT EXISTS idx_conversations_user_id ON conversations (user_id);

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    grounded BOOLEAN,
    citations TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages (conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_project_id ON messages (project_id);

-- ============ quizzes / questions / answers ============
CREATE TABLE IF NOT EXISTS quizzes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title VARCHAR(300) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    score DOUBLE PRECISION,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_quizzes_project_id ON quizzes (project_id);
CREATE INDEX IF NOT EXISTS idx_quizzes_user_id ON quizzes (user_id);

CREATE TABLE IF NOT EXISTS questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    concept_id UUID REFERENCES concepts(id) ON DELETE SET NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'MCQ',
    prompt TEXT NOT NULL,
    options TEXT,
    correct_answer TEXT,
    difficulty VARCHAR(20),
    position INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_questions_quiz_id ON questions (quiz_id);
CREATE INDEX IF NOT EXISTS idx_questions_project_id ON questions (project_id);

CREATE TABLE IF NOT EXISTS answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    quiz_id UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    answer_text TEXT,
    selected_option VARCHAR(10),
    score DOUBLE PRECISION,
    feedback TEXT,
    understood TEXT,
    missing TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_answers_question UNIQUE (question_id)
);
CREATE INDEX IF NOT EXISTS idx_answers_quiz_id ON answers (quiz_id);
CREATE INDEX IF NOT EXISTS idx_answers_user_id ON answers (user_id);
CREATE INDEX IF NOT EXISTS idx_answers_project_id ON answers (project_id);

-- ============ concept mastery ============
CREATE TABLE IF NOT EXISTS concept_mastery (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concept_id UUID NOT NULL REFERENCES concepts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    mastery_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    confidence DOUBLE PRECISION NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_mastery_concept UNIQUE (concept_id)
);
CREATE INDEX IF NOT EXISTS idx_mastery_project_id ON concept_mastery (project_id);
CREATE INDEX IF NOT EXISTS idx_mastery_user_id ON concept_mastery (user_id);

CREATE TABLE IF NOT EXISTS mastery_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concept_id UUID NOT NULL REFERENCES concepts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    score DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_mastery_history_concept ON mastery_history (concept_id);
CREATE INDEX IF NOT EXISTS idx_mastery_history_project ON mastery_history (project_id);
CREATE INDEX IF NOT EXISTS idx_mastery_history_created ON mastery_history (created_at);

-- ============ recommendations ============
CREATE TABLE IF NOT EXISTS recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_recommendations_project_id ON recommendations (project_id);
CREATE INDEX IF NOT EXISTS idx_recommendations_user_id ON recommendations (user_id);

-- ============ learning context (one row per project) ============
CREATE TABLE IF NOT EXISTS learning_context (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID NOT NULL UNIQUE REFERENCES projects(id) ON DELETE CASCADE,
    goals TEXT,
    strengths TEXT,
    weaknesses TEXT,
    repeated_mistakes TEXT,
    assessment_summary TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_learning_ctx_user ON learning_context (user_id);

-- ============ activity events (idempotent) ============
CREATE TABLE IF NOT EXISTS activity_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    event_type VARCHAR(60) NOT NULL,
    event_metadata TEXT,
    idempotency_key VARCHAR(200) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_events_user_id ON activity_events (user_id);
CREATE INDEX IF NOT EXISTS idx_events_project_id ON activity_events (project_id);
CREATE INDEX IF NOT EXISTS idx_events_created_at ON activity_events (created_at);
CREATE INDEX IF NOT EXISTS idx_events_type ON activity_events (event_type);

-- ============ ai usage ============
CREATE TABLE IF NOT EXISTS ai_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    feature VARCHAR(40) NOT NULL,
    model VARCHAR(120) NOT NULL,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    input_tokens INTEGER NOT NULL DEFAULT 0,
    output_tokens INTEGER NOT NULL DEFAULT 0,
    estimated_cost DOUBLE PRECISION NOT NULL DEFAULT 0,
    success BOOLEAN NOT NULL DEFAULT true,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_ai_usage_user ON ai_usage (user_id);
CREATE INDEX IF NOT EXISTS idx_ai_usage_project ON ai_usage (project_id);
CREATE INDEX IF NOT EXISTS idx_ai_usage_feature ON ai_usage (feature);
CREATE INDEX IF NOT EXISTS idx_ai_usage_created ON ai_usage (created_at);
