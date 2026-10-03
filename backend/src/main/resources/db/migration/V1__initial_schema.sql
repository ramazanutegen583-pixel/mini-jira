CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(16) NOT NULL CHECK (role IN ('ADMIN', 'USER'))
);
CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    priority VARCHAR(16) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    assignee_id UUID REFERENCES app_users(id),
    reporter_id UUID NOT NULL REFERENCES app_users(id),
    due_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX tasks_status_idx ON tasks(status);
CREATE INDEX tasks_assignee_idx ON tasks(assignee_id);
CREATE INDEX tasks_created_idx ON tasks(created_at);
