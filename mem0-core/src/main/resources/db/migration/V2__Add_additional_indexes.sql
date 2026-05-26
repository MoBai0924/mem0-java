-- Add composite indexes for common query patterns

-- Composite index for filtering memories by multiple scopes
CREATE INDEX idx_memories_user_agent ON memories(user_id, agent_id) WHERE user_id IS NOT NULL AND agent_id IS NOT NULL;
CREATE INDEX idx_memories_user_run ON memories(user_id, run_id) WHERE user_id IS NOT NULL AND run_id IS NOT NULL;
CREATE INDEX idx_memories_agent_run ON memories(agent_id, run_id) WHERE agent_id IS NOT NULL AND run_id IS NOT NULL;
CREATE INDEX idx_memories_all_scopes ON memories(user_id, agent_id, run_id) WHERE user_id IS NOT NULL AND agent_id IS NOT NULL AND run_id IS NOT NULL;

-- Index for active API keys lookup
CREATE INDEX idx_api_keys_active_expires ON api_keys(is_active, expires_at) WHERE is_active = true;

-- Index for non-revoked refresh tokens
CREATE INDEX idx_refresh_tokens_valid ON refresh_tokens(user_id, is_revoked, expires_at) WHERE is_revoked = false;

-- Partial index for active users
CREATE INDEX idx_users_active ON users(id) WHERE is_active = true;

-- Add function to calculate cosine similarity
CREATE OR REPLACE FUNCTION cosine_similarity(a vector, b vector)
RETURNS float AS $$
    SELECT 1 - (a <=> b)
$$ LANGUAGE SQL IMMUTABLE STRICT;

-- Add function for hybrid search score fusion
CREATE OR REPLACE FUNCTION hybrid_search_score(
    semantic_score float,
    keyword_score float,
    entity_score float,
    semantic_weight float DEFAULT 0.7,
    keyword_weight float DEFAULT 0.2,
    entity_weight float DEFAULT 0.1
)
RETURNS float AS $$
    SELECT (semantic_score * semantic_weight + keyword_score * keyword_weight + entity_score * entity_weight)
$$ LANGUAGE SQL IMMUTABLE;

-- Add comments
COMMENT ON FUNCTION cosine_similarity IS 'Calculate cosine similarity between two vectors (1 - cosine distance)';
COMMENT ON FUNCTION hybrid_search_score IS 'Calculate combined score for hybrid search with configurable weights';
