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

-- ── Row Level Security (RLS) Policies ────────────────
-- Critical for public open-source apps using Supabase client anon keys.

-- 1. Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE daily_checkins ENABLE ROW LEVEL SECURITY;
ALTER TABLE badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE plan_votes ENABLE ROW LEVEL SECURITY;
ALTER TABLE downloaded_plans ENABLE ROW LEVEL SECURITY;

-- 2. Users Table Policies
CREATE POLICY "Users can view their own profile"
    ON users FOR SELECT
    USING (auth.uid()::text = cloud_user_id);

CREATE POLICY "Users can update their own profile"
    ON users FOR UPDATE
    USING (auth.uid()::text = cloud_user_id);

CREATE POLICY "Users can insert their own profile"
    ON users FOR INSERT
    WITH CHECK (auth.uid()::text = cloud_user_id);

-- 3. Plans Table Policies
CREATE POLICY "Users can read their own plans or public community plans"
    ON plans FOR SELECT
    USING (
        is_public = TRUE 
        OR (user_id IN (SELECT user_id FROM users WHERE cloud_user_id = auth.uid()::text))
    );

CREATE POLICY "Users can create their own plans"
    ON plans FOR INSERT
    WITH CHECK (user_id IN (SELECT user_id FROM users WHERE cloud_user_id = auth.uid()::text));

CREATE POLICY "Users can update their own plans"
    ON plans FOR UPDATE
    USING (user_id IN (SELECT user_id FROM users WHERE cloud_user_id = auth.uid()::text));

CREATE POLICY "Users can delete their own plans"
    ON plans FOR DELETE
    USING (user_id IN (SELECT user_id FROM users WHERE cloud_user_id = auth.uid()::text));

-- 4. Task Templates Policies
CREATE POLICY "Users can view templates of visible plans"
    ON task_templates FOR SELECT
    USING (
        plan_id IN (
            SELECT plan_id FROM plans 
            WHERE is_public = TRUE 
               OR user_id IN (SELECT user_id FROM users WHERE cloud_user_id = auth.uid()::text)
        )
    );

CREATE POLICY "Users can modify templates of their own plans"
    ON task_templates FOR ALL
    USING (
        plan_id IN (
            SELECT plan_id FROM plans 
            WHERE user_id IN (SELECT user_id FROM users WHERE cloud_user_id = auth.uid()::text)
        )
    );

-- 5. Daily Check-ins Policies
CREATE POLICY "Users can manage checkins for their own templates"
    ON daily_checkins FOR ALL
    USING (
        template_id IN (
            SELECT template_id FROM task_templates
            WHERE plan_id IN (
                SELECT plan_id FROM plans 
                WHERE user_id IN (SELECT user_id FROM users WHERE cloud_user_id = auth.uid()::text)
            )
        )
    );

-- 6. Badges Policies
CREATE POLICY "Users can view their own badges"
    ON badges FOR SELECT
    USING (user_id IN (SELECT user_id FROM users WHERE cloud_user_id = auth.uid()::text));

-- 7. Plan Votes Policies
CREATE POLICY "Users can read all plan votes"
    ON plan_votes FOR SELECT
    USING (TRUE);

CREATE POLICY "Users can cast or change their own votes"
    ON plan_votes FOR ALL
    USING (user_id IN (SELECT user_id FROM users WHERE cloud_user_id = auth.uid()::text));

-- 8. Downloaded Plans Policies
CREATE POLICY "Users can view and manage their downloaded plans"
    ON downloaded_plans FOR ALL
    USING (user_id IN (SELECT user_id FROM users WHERE cloud_user_id = auth.uid()::text));
