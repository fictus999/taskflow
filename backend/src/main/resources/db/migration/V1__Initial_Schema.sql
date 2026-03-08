-- V1__Initial_Schema.sql
-- TaskFlow database schema

-- Users table
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    avatar_url    VARCHAR(500),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login    TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Projects table
CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    status      VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    deadline    TIMESTAMP,
    owner_id    BIGINT       NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Project members (M2M)
CREATE TABLE project_members (
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (project_id, user_id)
);

-- Tasks table
CREATE TABLE tasks (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    status           VARCHAR(30)  NOT NULL DEFAULT 'TODO',
    priority         VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    due_date         TIMESTAMP,
    story_points     INT,
    estimated_hours  DECIMAL(6,2),
    logged_hours     DECIMAL(6,2) NOT NULL DEFAULT 0,
    project_id       BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    assignee_id      BIGINT       REFERENCES users(id),
    creator_id       BIGINT       NOT NULL REFERENCES users(id),
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Comments
CREATE TABLE comments (
    id         BIGSERIAL PRIMARY KEY,
    content    TEXT      NOT NULL,
    task_id    BIGINT    NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    author_id  BIGINT    NOT NULL REFERENCES users(id),
    edited     BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Attachments
CREATE TABLE attachments (
    id           BIGSERIAL PRIMARY KEY,
    filename     VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    size_bytes   BIGINT,
    storage_key  VARCHAR(500) NOT NULL,
    task_id      BIGINT       NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    uploader_id  BIGINT       NOT NULL REFERENCES users(id),
    uploaded_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Audit log
CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   BIGINT       NOT NULL,
    action      VARCHAR(20)  NOT NULL,
    actor_id    BIGINT       REFERENCES users(id),
    diff_json   JSONB,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_tasks_project  ON tasks(project_id);
CREATE INDEX idx_tasks_assignee ON tasks(assignee_id);
CREATE INDEX idx_tasks_status   ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_comments_task  ON comments(task_id);
CREATE INDEX idx_audit_entity   ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_actor    ON audit_log(actor_id);

-- Sample data
INSERT INTO users (name, email, password_hash, role)
VALUES
  ('Admin User',   'admin@taskflow.dev',   '$2a$12$placeholder_bcrypt_hash', 'ADMIN'),
  ('Janu', 'manager@taskflow.dev', '$2a$12$placeholder_bcrypt_hash', 'MANAGER'),
  ('James',     'dev@taskflow.dev',     '$2a$12$placeholder_bcrypt_hash', 'USER');
```

