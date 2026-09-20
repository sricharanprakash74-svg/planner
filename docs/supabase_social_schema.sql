-- ==============================================================================
-- PlannerApp: Supabase PostgreSQL Social Platform Schema (Idempotent & Safe)
-- ==============================================================================
-- Run this script in your Supabase SQL Editor (https://supabase.com/dashboard)
-- It sets up the social platform tables, triggers, and RLS policies cleanly
-- without colliding with existing Room sync tables (e.g. plans, users, plan_votes).
-- ==============================================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- Fast fuzzy search on username, title, tags

-- 2. USER PROFILES TABLE
-- Linked to auth.users (Supabase Auth)
CREATE TABLE IF NOT EXISTS public.profiles (
    id              UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username        TEXT UNIQUE NOT NULL,
    display_name    TEXT NOT NULL DEFAULT 'Planner User',
    avatar_url      TEXT,
    bio             TEXT DEFAULT '',
    is_creator      BOOLEAN DEFAULT FALSE,
    followers_count INT DEFAULT 0,
    following_count INT DEFAULT 0,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_profiles_username ON public.profiles(username);
CREATE INDEX IF NOT EXISTS idx_profiles_trgm ON public.profiles USING gin (username gin_trgm_ops, display_name gin_trgm_ops);

-- Automatically create profile on new user signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, username, display_name, avatar_url)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'username', 'user_' || SUBSTRING(NEW.id::text, 1, 8)),
        COALESCE(NEW.raw_user_meta_data->>'display_name', NEW.raw_user_meta_data->>'full_name', 'Planner User'),
        NEW.raw_user_meta_data->>'avatar_url'
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 3. PUBLIC PLANS TABLE (Online Community Feed)
CREATE TABLE IF NOT EXISTS public.public_plans (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title               TEXT NOT NULL,
    description         TEXT DEFAULT '',
    category            TEXT NOT NULL DEFAULT 'General',
    tags                TEXT[] DEFAULT '{}',
    duration_days       INT NOT NULL DEFAULT 7,
    tasks_count         INT NOT NULL DEFAULT 1,
    template_json       JSONB NOT NULL DEFAULT '{}'::jsonb,
    upvote_count        INT NOT NULL DEFAULT 0,
    join_count          INT NOT NULL DEFAULT 0,
    comment_count       INT NOT NULL DEFAULT 0,
    is_public           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_plans_public_user ON public.public_plans(user_id);
CREATE INDEX IF NOT EXISTS idx_plans_visibility ON public.public_plans(is_public) WHERE is_public = TRUE;
CREATE INDEX IF NOT EXISTS idx_plans_search ON public.public_plans USING gin (title gin_trgm_ops, description gin_trgm_ops, category gin_trgm_ops);

-- 4. PUBLIC PLAN VOTES TABLE
-- Named public_plan_votes to avoid collision with local Room sync plan_votes (BIGINT)
CREATE TABLE IF NOT EXISTS public.public_plan_votes (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id     UUID NOT NULL REFERENCES public.public_plans(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    vote_type   TEXT NOT NULL CHECK (vote_type IN ('UP', 'DOWN')),
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(plan_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_pub_votes_plan_user ON public.public_plan_votes(plan_id, user_id);

-- 5. PLAN COMMENTS TABLE (Threaded discussions)
CREATE TABLE IF NOT EXISTS public.plan_comments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id             UUID NOT NULL REFERENCES public.public_plans(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    content             TEXT NOT NULL,
    parent_comment_id   UUID REFERENCES public.plan_comments(id) ON DELETE CASCADE,
    upvote_count        INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_comments_plan ON public.plan_comments(plan_id);

-- 6. USER FOLLOWS TABLE
CREATE TABLE IF NOT EXISTS public.user_follows (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    follower_id     UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    following_id    UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(follower_id, following_id),
    CHECK (follower_id <> following_id)
);

CREATE INDEX IF NOT EXISTS idx_follows_pair ON public.user_follows(follower_id, following_id);

-- 7. SAVED PLANS TABLE (Bookmarks)
CREATE TABLE IF NOT EXISTS public.saved_plans (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    plan_id     UUID NOT NULL REFERENCES public.public_plans(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, plan_id)
);

CREATE INDEX IF NOT EXISTS idx_saved_user ON public.saved_plans(user_id);

-- 8. PLAN USES / COPIES TABLE (Independent copy tracking)
CREATE TABLE IF NOT EXISTS public.plan_uses (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    plan_id     UUID NOT NULL REFERENCES public.public_plans(id) ON DELETE CASCADE,
    used_at     TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, plan_id)
);

-- 9. CONTENT REPORTS & MODERATION TABLE
CREATE TABLE IF NOT EXISTS public.content_reports (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id     UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    target_id       TEXT NOT NULL,
    target_type     TEXT NOT NULL CHECK (target_type IN ('PLAN', 'COMMENT', 'USER')),
    reason          TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'REVIEWED', 'DISMISSED')),
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- 10. USER BLOCKS TABLE
CREATE TABLE IF NOT EXISTS public.user_blocks (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    blocker_id      UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    blocked_id      UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);

-- ==============================================================================
-- AUTOMATIC COUNTER TRIGGERS
-- ==============================================================================

-- Maintain upvote_count on public_plans
CREATE OR REPLACE FUNCTION update_plan_upvote_count()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE public.public_plans SET upvote_count = upvote_count + 1 WHERE id = NEW.plan_id;
    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE public.public_plans SET upvote_count = GREATEST(0, upvote_count - 1) WHERE id = OLD.plan_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_vote_count ON public.public_plan_votes;
CREATE TRIGGER trg_vote_count
AFTER INSERT OR DELETE ON public.public_plan_votes
FOR EACH ROW EXECUTE FUNCTION update_plan_upvote_count();

-- Maintain comment_count on public_plans
CREATE OR REPLACE FUNCTION update_plan_comment_count()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE public.public_plans SET comment_count = comment_count + 1 WHERE id = NEW.plan_id;
    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE public.public_plans SET comment_count = GREATEST(0, comment_count - 1) WHERE id = OLD.plan_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_comment_count ON public.plan_comments;
CREATE TRIGGER trg_comment_count
AFTER INSERT OR DELETE ON public.plan_comments
FOR EACH ROW EXECUTE FUNCTION update_plan_comment_count();

-- Maintain join_count on public_plans
CREATE OR REPLACE FUNCTION update_plan_join_count()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE public.public_plans SET join_count = join_count + 1 WHERE id = NEW.plan_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_join_count ON public.plan_uses;
CREATE TRIGGER trg_join_count
AFTER INSERT ON public.plan_uses
FOR EACH ROW EXECUTE FUNCTION update_plan_join_count();

-- Maintain followers_count & following_count
CREATE OR REPLACE FUNCTION update_follow_counts()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        UPDATE public.profiles SET following_count = following_count + 1 WHERE id = NEW.follower_id;
        UPDATE public.profiles SET followers_count = followers_count + 1 WHERE id = NEW.following_id;
    ELSIF (TG_OP = 'DELETE') THEN
        UPDATE public.profiles SET following_count = GREATEST(0, following_count - 1) WHERE id = OLD.follower_id;
        UPDATE public.profiles SET followers_count = GREATEST(0, followers_count - 1) WHERE id = OLD.following_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_follow_counts ON public.user_follows;
CREATE TRIGGER trg_follow_counts
AFTER INSERT OR DELETE ON public.user_follows
FOR EACH ROW EXECUTE FUNCTION update_follow_counts();

-- ==============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.public_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.public_plan_votes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.plan_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.saved_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.plan_uses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.content_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_blocks ENABLE ROW LEVEL SECURITY;

-- Profiles: Anyone can view profiles; only owner can update
DROP POLICY IF EXISTS "Public profiles are viewable by everyone" ON public.profiles;
CREATE POLICY "Public profiles are viewable by everyone" ON public.profiles FOR SELECT USING (TRUE);

DROP POLICY IF EXISTS "Users can update their own profile" ON public.profiles;
CREATE POLICY "Users can update their own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);

DROP POLICY IF EXISTS "Users can insert their own profile" ON public.profiles;
CREATE POLICY "Users can insert their own profile" ON public.profiles FOR INSERT WITH CHECK (auth.uid() = id);

-- Public Plans: Public plans viewable by everyone; Private plans only by owner
DROP POLICY IF EXISTS "Public plans are viewable by everyone" ON public.public_plans;
CREATE POLICY "Public plans are viewable by everyone" ON public.public_plans FOR SELECT 
    USING (is_public = TRUE OR auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert their own plans" ON public.public_plans;
CREATE POLICY "Users can insert their own plans" ON public.public_plans FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update their own plans" ON public.public_plans;
CREATE POLICY "Users can update their own plans" ON public.public_plans FOR UPDATE USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete their own plans" ON public.public_plans;
CREATE POLICY "Users can delete their own plans" ON public.public_plans FOR DELETE USING (auth.uid() = user_id);

-- Votes: Viewable by everyone; cast/cancel by authenticated user
DROP POLICY IF EXISTS "Votes are viewable by everyone" ON public.public_plan_votes;
CREATE POLICY "Votes are viewable by everyone" ON public.public_plan_votes FOR SELECT USING (TRUE);

DROP POLICY IF EXISTS "Users can vote" ON public.public_plan_votes;
CREATE POLICY "Users can vote" ON public.public_plan_votes FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can remove their vote" ON public.public_plan_votes;
CREATE POLICY "Users can remove their vote" ON public.public_plan_votes FOR DELETE USING (auth.uid() = user_id);

-- Comments: Viewable by everyone; author can create/delete
DROP POLICY IF EXISTS "Comments are viewable by everyone" ON public.plan_comments;
CREATE POLICY "Comments are viewable by everyone" ON public.plan_comments FOR SELECT USING (TRUE);

DROP POLICY IF EXISTS "Authenticated users can comment" ON public.plan_comments;
CREATE POLICY "Authenticated users can comment" ON public.plan_comments FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete their own comments" ON public.plan_comments;
CREATE POLICY "Users can delete their own comments" ON public.plan_comments FOR DELETE USING (auth.uid() = user_id);

-- Follows: Viewable by everyone; user can follow/unfollow
DROP POLICY IF EXISTS "Follows are viewable by everyone" ON public.user_follows;
CREATE POLICY "Follows are viewable by everyone" ON public.user_follows FOR SELECT USING (TRUE);

DROP POLICY IF EXISTS "Users can follow others" ON public.user_follows;
CREATE POLICY "Users can follow others" ON public.user_follows FOR INSERT WITH CHECK (auth.uid() = follower_id);

DROP POLICY IF EXISTS "Users can unfollow others" ON public.user_follows;
CREATE POLICY "Users can unfollow others" ON public.user_follows FOR DELETE USING (auth.uid() = follower_id);

-- Saved Plans: Only visible to the owner
DROP POLICY IF EXISTS "Users can view their saved plans" ON public.saved_plans;
CREATE POLICY "Users can view their saved plans" ON public.saved_plans FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can save plans" ON public.saved_plans;
CREATE POLICY "Users can save plans" ON public.saved_plans FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can unsave plans" ON public.saved_plans;
CREATE POLICY "Users can unsave plans" ON public.saved_plans FOR DELETE USING (auth.uid() = user_id);

-- Plan Uses: Authenticated user can record usage
DROP POLICY IF EXISTS "Users can record plan usage" ON public.plan_uses;
CREATE POLICY "Users can record plan usage" ON public.plan_uses FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Plan usage viewable by everyone" ON public.plan_uses;
CREATE POLICY "Plan usage viewable by everyone" ON public.plan_uses FOR SELECT USING (TRUE);

-- Reports & Blocks
DROP POLICY IF EXISTS "Users can report content" ON public.content_reports;
CREATE POLICY "Users can report content" ON public.content_reports FOR INSERT WITH CHECK (auth.uid() = reporter_id);

DROP POLICY IF EXISTS "Users can block others" ON public.user_blocks;
CREATE POLICY "Users can block others" ON public.user_blocks FOR ALL USING (auth.uid() = blocker_id);
