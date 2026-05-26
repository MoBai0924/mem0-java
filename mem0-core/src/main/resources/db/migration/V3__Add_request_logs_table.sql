-- Request logs table for API request logging
CREATE TABLE IF NOT EXISTS request_logs (
    id UUID PRIMARY KEY,
    method VARCHAR(10) NOT NULL,
    path TEXT NOT NULL,
    query_string TEXT,
    status_code INTEGER NOT NULL,
    user_id UUID,
    api_key_id UUID,
    client_ip VARCHAR(45),
    duration_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for request log queries
CREATE INDEX IF NOT EXISTS idx_request_logs_user_id ON request_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_request_logs_method ON request_logs (method);
CREATE INDEX IF NOT EXISTS idx_request_logs_status_code ON request_logs (status_code);
CREATE INDEX IF NOT EXISTS idx_request_logs_created_at ON request_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_request_logs_path ON request_logs (path);
