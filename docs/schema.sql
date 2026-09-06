-- ============================================================
-- PlannerApp: PostgreSQL Cloud Schema (Reference)
-- Generated alongside Room entities for contract parity.
-- ============================================================

-- ── Users ───────────────────────────────────────────
CREATE TABLE users (
    user_id         BIGSERIAL PRIMARY KEY,
    cloud_user_id   TEXT UNIQUE,                           -- OAuth / Firebase UID
    auth_provider   TEXT DEFAULT 'email',                  -- 'google' | 'email'
    display_name    TEXT NOT NULL DEFAULT 'Guest',
    email           TEXT UNIQUE,
    avatar_url      TEXT,
    password_hash   TEXT,                                  -- NULL for OAuth users
    user_timezone   TEXT NOT NULL DEFAULT 'UTC',           -- e.g., 'Asia/Kolkata'
    is_creator      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_cloud_id ON users(cloud_user_id);

-- ── Plans ───────────────────────────────────────────
CREATE TABLE plans (
    plan_id         BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    heading         TEXT NOT NULL,
    description     TEXT NOT NULL DEFAULT '',
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    is_public       BOOLEAN NOT NULL DEFAULT FALSE,
    source_plan_id  BIGINT REFERENCES plans(plan_id) ON DELETE SET NULL,  -- Immutable clone tracking
    upvote_count    INT NOT NULL DEFAULT 0,
    download_count  INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_plans_user ON plans(user_id);
CREATE INDEX idx_plans_public ON plans(is_public) WHERE is_public = TRUE;

-- ── Task Templates ──────────────────────────────────
CREATE TABLE task_templates (
    template_id     BIGSERIAL PRIMARY KEY,
    plan_id         BIGINT NOT NULL REFERENCES plans(plan_id) ON DELETE CASCADE,
    task_description TEXT NOT NULL,
    selected_days   TEXT NOT NULL,                         -- e.g., '1,3,5'
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_templates_plan ON task_templates(plan_id);

-- ── Daily Check-ins ─────────────────────────────────
CREATE TABLE daily_checkins (
    checkin_id      BIGSERIAL PRIMARY KEY,
    template_id     BIGINT NOT NULL REFERENCES task_templates(template_id) ON DELETE CASCADE,
    exact_date      DATE NOT NULL,
    is_completed    BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at    TIMESTAMPTZ,                           -- When the user marked it done
    timezone_offset TEXT NOT NULL DEFAULT '',               -- e.g., '+05:30'
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_checkins_template ON daily_checkins(template_id);
CREATE INDEX idx_checkins_date ON daily_checkins(exact_date);
-- Composite index for the widget/home query pattern
CREATE INDEX idx_checkins_date_template ON daily_checkins(exact_date, template_id);

-- ── Badges ──────────────────────────────────────────
CREATE TABLE badges (
    badge_id        BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    badge_type      TEXT NOT NULL,                         -- 'STREAK_7', 'STREAK_30', 'FIRST_PLAN', 'CREATOR'
    unlocked_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, badge_type)                            -- Prevent duplicate badge awards
);

CREATE INDEX idx_badges_user ON badges(user_id);

-- ── Plan Votes ──────────────────────────────────────
CREATE TABLE plan_votes (
    vote_id         BIGSERIAL PRIMARY KEY,
    plan_id         BIGINT NOT NULL REFERENCES plans(plan_id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    vote_value      INT NOT NULL CHECK (vote_value IN (1, -1)),  -- +1 upvote, -1 downvote
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(plan_id, user_id)                               -- One vote per user per plan
);

CREATE INDEX idx_votes_plan ON plan_votes(plan_id);
CREATE INDEX idx_votes_user ON plan_votes(user_id);

-- ── Downloaded Plans (Junction Table) ───────────────
CREATE TABLE downloaded_plans (
    download_id     BIGSERIAL PRIMARY KEY,
    original_plan_id BIGINT NOT NULL REFERENCES plans(plan_id) ON DELETE SET NULL,
    cloned_plan_id  BIGINT NOT NULL REFERENCES plans(plan_id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    downloaded_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(original_plan_id, user_id)                      -- User can only download a plan once
);

CREATE INDEX idx_downloads_user ON downloaded_plans(user_id);

-- ── Utility Functions ───────────────────────────────

-- Auto-update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();

CREATE TRIGGER trg_plans_updated BEFORE UPDATE ON plans
    FOR EACH ROW EXECUTE FUNCTION update_modified_column();
