-- ==============================================================================
-- PlannerApp: Supabase PostgreSQL Social Planning Platform Schema
-- ==============================================================================
-- Idempotent, production-ready schema for the plan-centric social platform.
-- The central social object is the PLAN.
-- ==============================================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- 2. USER PROFILES
-- Separation of auth identity (auth.users) and public profile identity.
CREATE TABLE IF NOT EXISTS public.profiles (
    id              UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username        TEXT UNIQUE NOT NULL,
    display_name    TEXT NOT NULL DEFAULT 'Planner User',
    avatar_url      TEXT,
    bio             TEXT DEFAULT '',
    is_creator      BOOLEAN DEFAULT FALSE,
    followers_count INT DEFAULT 0,
    following_count INT DEFAULT 0,
    public_plans_count INT DEFAULT 0,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_profiles_username ON public.profiles(username);
CREATE INDEX IF NOT EXISTS idx_profiles_trgm ON public.profiles USING gin (username gin_trgm_ops, display_name gin_trgm_ops);

-- Profile creation trigger from auth.users
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

    INSERT INTO public.privacy_settings (user_id)
    VALUES (NEW.id)
    ON CONFLICT (user_id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. USER PRIVACY SETTINGS
CREATE TABLE IF NOT EXISTS public.privacy_settings (
    user_id                 UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    profile_visibility      TEXT NOT NULL DEFAULT 'PUBLIC' CHECK (profile_visibility IN ('PUBLIC', 'FOLLOWERS_ONLY', 'PRIVATE')),
    allow_messages_from     TEXT NOT NULL DEFAULT 'EVERYONE' CHECK (allow_messages_from IN ('EVERYONE', 'FOLLOWED', 'NONE')),
    allow_comments_from     TEXT NOT NULL DEFAULT 'EVERYONE' CHECK (allow_comments_from IN ('EVERYONE', 'FOLLOWED', 'NONE')),
    show_activity_status    BOOLEAN NOT NULL DEFAULT TRUE,
    default_plan_visibility TEXT NOT NULL DEFAULT 'PUBLIC' CHECK (default_plan_visibility IN ('PUBLIC', 'UNLISTED', 'PRIVATE')),
    updated_at              TIMESTAMPTZ DEFAULT NOW()
);

-- Re-hook auth trigger
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 4. PUBLIC PLANS (The Central Social Object)
CREATE TABLE IF NOT EXISTS public.public_plans (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    creator_id          UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title               TEXT NOT NULL,
    description         TEXT DEFAULT '',
    category            TEXT NOT NULL DEFAULT 'General',
    tags                TEXT[] DEFAULT '{}',
    duration_days       INT NOT NULL DEFAULT 7,
    current_version     TEXT NOT NULL DEFAULT '1.0.0',
    saves_count         INT NOT NULL DEFAULT 0,
    uses_count          INT NOT NULL DEFAULT 0,
    follows_count       INT NOT NULL DEFAULT 0,
    likes_count         INT NOT NULL DEFAULT 0,
    comments_count      INT NOT NULL DEFAULT 0,
    visibility          TEXT NOT NULL DEFAULT 'PUBLIC' CHECK (visibility IN ('PUBLIC', 'FOLLOWERS_ONLY', 'UNLISTED', 'ARCHIVED')),
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_plans_creator ON public.public_plans(creator_id);
CREATE INDEX IF NOT EXISTS idx_plans_visibility ON public.public_plans(visibility) WHERE visibility = 'PUBLIC';
CREATE INDEX IF NOT EXISTS idx_plans_search ON public.public_plans USING gin (title gin_trgm_ops, description gin_trgm_ops, category gin_trgm_ops);

-- 5. PLAN VERSIONS (Immutable snapshots ensuring historical integrity)
CREATE TABLE IF NOT EXISTS public.plan_versions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id         UUID NOT NULL REFERENCES public.public_plans(id) ON DELETE CASCADE,
    version_tag     TEXT NOT NULL, -- e.g., '1.0.0'
    changelog       TEXT DEFAULT 'Initial publication',
    template_json   JSONB NOT NULL,
    published_at    TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(plan_id, version_tag)
);

CREATE INDEX IF NOT EXISTS idx_plan_versions_plan ON public.plan_versions(plan_id);

-- 6. DISTINCT SOCIAL RELATIONSHIPS

-- Relationship A: SAVE (Retain for later reference)
CREATE TABLE IF NOT EXISTS public.plan_saves (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    plan_id     UUID NOT NULL REFERENCES public.public_plans(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, plan_id)
);
CREATE INDEX IF NOT EXISTS idx_plan_saves_user ON public.plan_saves(user_id);
CREATE INDEX IF NOT EXISTS idx_plan_saves_plan ON public.plan_saves(plan_id);

-- Relationship B: USE (Activation of an independent personal instance)
CREATE TABLE IF NOT EXISTS public.plan_uses (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    plan_id     UUID NOT NULL REFERENCES public.public_plans(id) ON DELETE CASCADE,
    version_id  UUID REFERENCES public.plan_versions(id) ON DELETE SET NULL,
    used_at     TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_plan_uses_user ON public.plan_uses(user_id);
CREATE INDEX IF NOT EXISTS idx_plan_uses_plan ON public.plan_uses(plan_id);

-- Relationship C: PLAN FOLLOW (Ongoing relationship with a specific plan)
CREATE TABLE IF NOT EXISTS public.plan_follows (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    plan_id     UUID NOT NULL REFERENCES public.public_plans(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, plan_id)
);
CREATE INDEX IF NOT EXISTS idx_plan_follows_user ON public.plan_follows(user_id);
CREATE INDEX IF NOT EXISTS idx_plan_follows_plan ON public.plan_follows(plan_id);

-- Relationship D: USER FOLLOW (Social graph between users/creators)
CREATE TABLE IF NOT EXISTS public.user_follows (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    follower_id     UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    following_id    UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(follower_id, following_id),
    CHECK (follower_id <> following_id)
);
CREATE INDEX IF NOT EXISTS idx_user_follows_pair ON public.user_follows(follower_id, following_id);

-- Relationship E: PLAN LIKE / UPVOTE (Lightweight appreciation)
CREATE TABLE IF NOT EXISTS public.plan_likes (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    plan_id     UUID NOT NULL REFERENCES public.public_plans(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, plan_id)
);
CREATE INDEX IF NOT EXISTS idx_plan_likes_plan_user ON public.plan_likes(plan_id, user_id);

-- 7. POSTS (Secondary Social Content referencing a plan)
CREATE TABLE IF NOT EXISTS public.posts (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    author_id           UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    referenced_plan_id  UUID REFERENCES public.public_plans(id) ON DELETE SET NULL,
    title               TEXT NOT NULL,
    content             TEXT NOT NULL,
    likes_count         INT NOT NULL DEFAULT 0,
    comments_count      INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_posts_author ON public.posts(author_id);
CREATE INDEX IF NOT EXISTS idx_posts_plan ON public.posts(referenced_plan_id);

-- Post Likes
CREATE TABLE IF NOT EXISTS public.post_likes (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    post_id     UUID NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, post_id)
);
CREATE INDEX IF NOT EXISTS idx_post_likes_pair ON public.post_likes(post_id, user_id);

-- 8. COMMENTS (Discussions on Plans or Posts)
CREATE TABLE IF NOT EXISTS public.comments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    plan_id             UUID REFERENCES public.public_plans(id) ON DELETE CASCADE,
    post_id             UUID REFERENCES public.posts(id) ON DELETE CASCADE,
    parent_comment_id   UUID REFERENCES public.comments(id) ON DELETE CASCADE,
    content             TEXT NOT NULL,
    likes_count         INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW(),
    CHECK (plan_id IS NOT NULL OR post_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_comments_plan ON public.comments(plan_id);
CREATE INDEX IF NOT EXISTS idx_comments_post ON public.comments(post_id);

-- Comment Likes
CREATE TABLE IF NOT EXISTS public.comment_likes (
    comment_id UUID NOT NULL REFERENCES public.comments(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (comment_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_comment_likes_pair ON public.comment_likes(comment_id, user_id);

-- 9. MESSAGING ARCHITECTURE (Private communication)
CREATE TABLE IF NOT EXISTS public.conversations (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.conversation_members (
    conversation_id UUID NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    joined_at       TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.messages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    content         TEXT NOT NULL,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation ON public.messages(conversation_id, created_at DESC);

-- 10. NOTIFICATIONS
CREATE TABLE IF NOT EXISTS public.notifications (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipient_id    UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    actor_id        UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    event_type      TEXT NOT NULL CHECK (event_type IN ('USER_FOLLOW', 'PLAN_FOLLOW', 'PLAN_SAVE', 'PLAN_USE', 'PLAN_LIKE', 'PLAN_COMMENT', 'POST_LIKE', 'POST_COMMENT', 'MESSAGE')),
    entity_id       TEXT NOT NULL, -- UUID or reference of target object
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON public.notifications(recipient_id, created_at DESC);

-- 11. BLOCKING & MODERATION
CREATE TABLE IF NOT EXISTS public.user_blocks (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    blocker_id  UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    blocked_id  UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);

CREATE INDEX IF NOT EXISTS idx_blocks_pair ON public.user_blocks(blocker_id, blocked_id);

CREATE TABLE IF NOT EXISTS public.content_reports (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id     UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    target_id       TEXT NOT NULL,
    target_type     TEXT NOT NULL CHECK (target_type IN ('PLAN', 'POST', 'COMMENT', 'USER', 'MESSAGE')),
    reason          TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'REVIEWED', 'DISMISSED')),
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- AUTOMATED COUNTER TRIGGERS
-- ==============================================================================

-- Maintain public_plans counts
CREATE OR REPLACE FUNCTION update_plan_counters()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_TABLE_NAME = 'plan_saves') THEN
        IF (TG_OP = 'INSERT') THEN
            UPDATE public.public_plans SET saves_count = saves_count + 1 WHERE id = NEW.plan_id;
        ELSIF (TG_OP = 'DELETE') THEN
            UPDATE public.public_plans SET saves_count = GREATEST(0, saves_count - 1) WHERE id = OLD.plan_id;
        END IF;
    ELSIF (TG_TABLE_NAME = 'plan_uses') THEN
        IF (TG_OP = 'INSERT') THEN
            UPDATE public.public_plans SET uses_count = uses_count + 1 WHERE id = NEW.plan_id;
        END IF;
    ELSIF (TG_TABLE_NAME = 'plan_follows') THEN
        IF (TG_OP = 'INSERT') THEN
            UPDATE public.public_plans SET follows_count = follows_count + 1 WHERE id = NEW.plan_id;
        ELSIF (TG_OP = 'DELETE') THEN
            UPDATE public.public_plans SET follows_count = GREATEST(0, follows_count - 1) WHERE id = OLD.plan_id;
        END IF;
    ELSIF (TG_TABLE_NAME = 'plan_likes') THEN
        IF (TG_OP = 'INSERT') THEN
            UPDATE public.public_plans SET likes_count = likes_count + 1 WHERE id = NEW.plan_id;
        ELSIF (TG_OP = 'DELETE') THEN
            UPDATE public.public_plans SET likes_count = GREATEST(0, likes_count - 1) WHERE id = OLD.plan_id;
        END IF;
    ELSIF (TG_TABLE_NAME = 'comments') THEN
        IF (TG_OP = 'INSERT' AND NEW.plan_id IS NOT NULL) THEN
            UPDATE public.public_plans SET comments_count = comments_count + 1 WHERE id = NEW.plan_id;
        ELSIF (TG_OP = 'DELETE' AND OLD.plan_id IS NOT NULL) THEN
            UPDATE public.public_plans SET comments_count = GREATEST(0, comments_count - 1) WHERE id = OLD.plan_id;
        END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_plan_saves_count ON public.plan_saves;
CREATE TRIGGER trg_plan_saves_count AFTER INSERT OR DELETE ON public.plan_saves FOR EACH ROW EXECUTE FUNCTION update_plan_counters();

DROP TRIGGER IF EXISTS trg_plan_uses_count ON public.plan_uses;
CREATE TRIGGER trg_plan_uses_count AFTER INSERT ON public.plan_uses FOR EACH ROW EXECUTE FUNCTION update_plan_counters();

DROP TRIGGER IF EXISTS trg_plan_follows_count ON public.plan_follows;
CREATE TRIGGER trg_plan_follows_count AFTER INSERT OR DELETE ON public.plan_follows FOR EACH ROW EXECUTE FUNCTION update_plan_counters();

DROP TRIGGER IF EXISTS trg_plan_likes_count ON public.plan_likes;
CREATE TRIGGER trg_plan_likes_count AFTER INSERT OR DELETE ON public.plan_likes FOR EACH ROW EXECUTE FUNCTION update_plan_counters();

DROP TRIGGER IF EXISTS trg_plan_comments_count ON public.comments;
CREATE TRIGGER trg_plan_comments_count AFTER INSERT OR DELETE ON public.comments FOR EACH ROW EXECUTE FUNCTION update_plan_counters();

-- Maintain user followers/following counts
CREATE OR REPLACE FUNCTION update_user_follow_counters()
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

DROP TRIGGER IF EXISTS trg_user_follows_count ON public.user_follows;
CREATE TRIGGER trg_user_follows_count AFTER INSERT OR DELETE ON public.user_follows FOR EACH ROW EXECUTE FUNCTION update_user_follow_counters();

-- Maintain creator public_plans_count
CREATE OR REPLACE FUNCTION update_creator_plan_count()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT' AND NEW.visibility = 'PUBLIC') THEN
        UPDATE public.profiles SET public_plans_count = public_plans_count + 1 WHERE id = NEW.creator_id;
    ELSIF (TG_OP = 'DELETE' AND OLD.visibility = 'PUBLIC') THEN
        UPDATE public.profiles SET public_plans_count = GREATEST(0, public_plans_count - 1) WHERE id = OLD.creator_id;
    ELSIF (TG_OP = 'UPDATE') THEN
        IF (OLD.visibility <> 'PUBLIC' AND NEW.visibility = 'PUBLIC') THEN
            UPDATE public.profiles SET public_plans_count = public_plans_count + 1 WHERE id = NEW.creator_id;
        ELSIF (OLD.visibility = 'PUBLIC' AND NEW.visibility <> 'PUBLIC') THEN
            UPDATE public.profiles SET public_plans_count = GREATEST(0, public_plans_count - 1) WHERE id = NEW.creator_id;
        END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_creator_plans_count ON public.public_plans;
CREATE TRIGGER trg_creator_plans_count AFTER INSERT OR UPDATE OR DELETE ON public.public_plans FOR EACH ROW EXECUTE FUNCTION update_creator_plan_count();

-- Maintain post counters
CREATE OR REPLACE FUNCTION update_post_counters()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_TABLE_NAME = 'post_likes') THEN
        IF (TG_OP = 'INSERT') THEN
            UPDATE public.posts SET likes_count = likes_count + 1 WHERE id = NEW.post_id;
        ELSIF (TG_OP = 'DELETE') THEN
            UPDATE public.posts SET likes_count = GREATEST(0, likes_count - 1) WHERE id = OLD.post_id;
        END IF;
    ELSIF (TG_TABLE_NAME = 'comments') THEN
        IF (TG_OP = 'INSERT' AND NEW.post_id IS NOT NULL) THEN
            UPDATE public.posts SET comments_count = comments_count + 1 WHERE id = NEW.post_id;
        ELSIF (TG_OP = 'DELETE' AND OLD.post_id IS NOT NULL) THEN
            UPDATE public.posts SET comments_count = GREATEST(0, comments_count - 1) WHERE id = OLD.post_id;
        END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_post_likes_count ON public.post_likes;
CREATE TRIGGER trg_post_likes_count AFTER INSERT OR DELETE ON public.post_likes FOR EACH ROW EXECUTE FUNCTION update_post_counters();

DROP TRIGGER IF EXISTS trg_post_comments_count ON public.comments;
CREATE TRIGGER trg_post_comments_count AFTER INSERT OR DELETE ON public.comments FOR EACH ROW EXECUTE FUNCTION update_post_counters();

-- ==============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.privacy_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.public_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.plan_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.plan_saves ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.plan_uses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.plan_follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.plan_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.post_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversation_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_blocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.content_reports ENABLE ROW LEVEL SECURITY;

-- Helper function: is user blocked?
CREATE OR REPLACE FUNCTION public.is_user_blocked(user1 UUID, user2 UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.user_blocks
        WHERE (blocker_id = user1 AND blocked_id = user2)
           OR (blocker_id = user2 AND blocked_id = user1)
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

-- Profiles Policies
DROP POLICY IF EXISTS "Profiles viewable respecting blocks" ON public.profiles;
CREATE POLICY "Profiles viewable respecting blocks" ON public.profiles FOR SELECT
    USING (NOT public.is_user_blocked(auth.uid(), id));

DROP POLICY IF EXISTS "Users update own profile" ON public.profiles;
CREATE POLICY "Users update own profile" ON public.profiles FOR UPDATE
    USING (auth.uid() = id);

-- Privacy Settings Policies
DROP POLICY IF EXISTS "Users manage own privacy settings" ON public.privacy_settings;
CREATE POLICY "Users manage own privacy settings" ON public.privacy_settings FOR ALL
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Others view privacy settings for policy enforcement" ON public.privacy_settings;
CREATE POLICY "Others view privacy settings for policy enforcement" ON public.privacy_settings FOR SELECT
    USING (TRUE);

-- Public Plans Policies
DROP POLICY IF EXISTS "Public plans viewable" ON public.public_plans;
CREATE POLICY "Public plans viewable" ON public.public_plans FOR SELECT
    USING (
        (visibility = 'PUBLIC' OR creator_id = auth.uid())
        AND NOT public.is_user_blocked(auth.uid(), creator_id)
    );

DROP POLICY IF EXISTS "Creators manage their plans" ON public.public_plans;
CREATE POLICY "Creators manage their plans" ON public.public_plans FOR ALL
    USING (auth.uid() = creator_id);

-- Plan Versions Policies
DROP POLICY IF EXISTS "Versions viewable for visible plans" ON public.plan_versions;
CREATE POLICY "Versions viewable for visible plans" ON public.plan_versions FOR SELECT
    USING (EXISTS (
        SELECT 1 FROM public.public_plans p
        WHERE p.id = plan_id
          AND (p.visibility = 'PUBLIC' OR p.creator_id = auth.uid())
          AND NOT public.is_user_blocked(auth.uid(), p.creator_id)
    ));

DROP POLICY IF EXISTS "Creators create versions for own plans" ON public.plan_versions;
CREATE POLICY "Creators create versions for own plans" ON public.plan_versions FOR INSERT
    WITH CHECK (EXISTS (
        SELECT 1 FROM public.public_plans p
        WHERE p.id = plan_id AND p.creator_id = auth.uid()
    ));

-- Plan Saves Policies
DROP POLICY IF EXISTS "Users view own saved plans" ON public.plan_saves;
CREATE POLICY "Users view own saved plans" ON public.plan_saves FOR SELECT
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users save plans" ON public.plan_saves;
CREATE POLICY "Users save plans" ON public.plan_saves FOR INSERT
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users unsave plans" ON public.plan_saves;
CREATE POLICY "Users unsave plans" ON public.plan_saves FOR DELETE
    USING (auth.uid() = user_id);

-- Plan Uses Policies
DROP POLICY IF EXISTS "Users view own plan uses" ON public.plan_uses;
CREATE POLICY "Users view own plan uses" ON public.plan_uses FOR SELECT
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users record plan usage" ON public.plan_uses;
CREATE POLICY "Users record plan usage" ON public.plan_uses FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Plan Follows Policies
DROP POLICY IF EXISTS "Users view own followed plans" ON public.plan_follows;
CREATE POLICY "Users view own followed plans" ON public.plan_follows FOR SELECT
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users follow plans" ON public.plan_follows;
CREATE POLICY "Users follow plans" ON public.plan_follows FOR INSERT
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users unfollow plans" ON public.plan_follows;
CREATE POLICY "Users unfollow plans" ON public.plan_follows FOR DELETE
    USING (auth.uid() = user_id);

-- Plan Likes Policies
DROP POLICY IF EXISTS "Plan likes viewable" ON public.plan_likes;
CREATE POLICY "Plan likes viewable" ON public.plan_likes FOR SELECT
    USING (TRUE);

DROP POLICY IF EXISTS "Users like plans" ON public.plan_likes;
CREATE POLICY "Users like plans" ON public.plan_likes FOR INSERT
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users unlike plans" ON public.plan_likes;
CREATE POLICY "Users unlike plans" ON public.plan_likes FOR DELETE
    USING (auth.uid() = user_id);

-- User Follows Policies
DROP POLICY IF EXISTS "User follows viewable" ON public.user_follows;
CREATE POLICY "User follows viewable" ON public.user_follows FOR SELECT
    USING (NOT public.is_user_blocked(auth.uid(), follower_id) AND NOT public.is_user_blocked(auth.uid(), following_id));

DROP POLICY IF EXISTS "Users follow other users" ON public.user_follows;
CREATE POLICY "Users follow other users" ON public.user_follows FOR INSERT
    WITH CHECK (auth.uid() = follower_id AND NOT public.is_user_blocked(follower_id, following_id));

DROP POLICY IF EXISTS "Users unfollow other users" ON public.user_follows;
CREATE POLICY "Users unfollow other users" ON public.user_follows FOR DELETE
    USING (auth.uid() = follower_id);

-- Posts Policies
DROP POLICY IF EXISTS "Posts viewable" ON public.posts;
CREATE POLICY "Posts viewable" ON public.posts FOR SELECT
    USING (NOT public.is_user_blocked(auth.uid(), author_id));

DROP POLICY IF EXISTS "Authors manage posts" ON public.posts;
CREATE POLICY "Authors manage posts" ON public.posts FOR ALL
    USING (auth.uid() = author_id);

-- Post Likes Policies
DROP POLICY IF EXISTS "Post likes viewable" ON public.post_likes;
CREATE POLICY "Post likes viewable" ON public.post_likes FOR SELECT
    USING (TRUE);

DROP POLICY IF EXISTS "Users like posts" ON public.post_likes;
CREATE POLICY "Users like posts" ON public.post_likes FOR INSERT
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users unlike posts" ON public.post_likes;
CREATE POLICY "Users unlike posts" ON public.post_likes FOR DELETE
    USING (auth.uid() = user_id);

-- Comments Policies
DROP POLICY IF EXISTS "Comments viewable" ON public.comments;
CREATE POLICY "Comments viewable" ON public.comments FOR SELECT
    USING (NOT public.is_user_blocked(auth.uid(), user_id));

DROP POLICY IF EXISTS "Users create comments" ON public.comments;
CREATE POLICY "Users create comments" ON public.comments FOR INSERT
    WITH CHECK (auth.uid() = user_id AND NOT public.is_user_blocked(auth.uid(), user_id));

DROP POLICY IF EXISTS "Users delete own comments" ON public.comments;
CREATE POLICY "Users delete own comments" ON public.comments FOR DELETE
    USING (auth.uid() = user_id);

-- Helper function to break infinite RLS recursion on conversation_members (42P17)
CREATE OR REPLACE FUNCTION public.is_conversation_member(p_conv_id UUID, p_user_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.conversation_members
        WHERE conversation_id = p_conv_id AND user_id = p_user_id
    );
$$;

-- Conversations Policies
DROP POLICY IF EXISTS "Users view conversations they belong to" ON public.conversations;
CREATE POLICY "Users view conversations they belong to" ON public.conversations FOR SELECT
    USING (public.is_conversation_member(id, auth.uid()));

DROP POLICY IF EXISTS "Users create conversations" ON public.conversations;
CREATE POLICY "Users create conversations" ON public.conversations FOR INSERT
    WITH CHECK (TRUE);

-- Conversation Members Policies
DROP POLICY IF EXISTS "Users view members of their conversations" ON public.conversation_members;
CREATE POLICY "Users view members of their conversations" ON public.conversation_members FOR SELECT
    USING (public.is_conversation_member(conversation_id, auth.uid()));

DROP POLICY IF EXISTS "Users add conversation members" ON public.conversation_members;
CREATE POLICY "Users add conversation members" ON public.conversation_members FOR INSERT
    WITH CHECK (auth.uid() = user_id OR public.is_conversation_member(conversation_id, auth.uid()));

-- Messages Policies
DROP POLICY IF EXISTS "Users view messages in their conversations" ON public.messages;
CREATE POLICY "Users view messages in their conversations" ON public.messages FOR SELECT
    USING (public.is_conversation_member(conversation_id, auth.uid()));

DROP POLICY IF EXISTS "Users send messages to their conversations" ON public.messages;
CREATE POLICY "Users send messages to their conversations" ON public.messages FOR INSERT
    WITH CHECK (
        auth.uid() = sender_id AND
        public.is_conversation_member(conversation_id, auth.uid())
    );

-- Notifications Policies
DROP POLICY IF EXISTS "Users view own notifications" ON public.notifications;
CREATE POLICY "Users view own notifications" ON public.notifications FOR SELECT
    USING (auth.uid() = recipient_id);

DROP POLICY IF EXISTS "Users update own notifications" ON public.notifications;
CREATE POLICY "Users update own notifications" ON public.notifications FOR UPDATE
    USING (auth.uid() = recipient_id);

DROP POLICY IF EXISTS "System creates notifications" ON public.notifications;
CREATE POLICY "System creates notifications" ON public.notifications FOR INSERT
    WITH CHECK (auth.uid() = actor_id);

-- Blocks & Reports Policies
DROP POLICY IF EXISTS "Users view and manage their blocks" ON public.user_blocks;
CREATE POLICY "Users view and manage their blocks" ON public.user_blocks FOR ALL
    USING (auth.uid() = blocker_id);

DROP POLICY IF EXISTS "Users submit content reports" ON public.content_reports;
CREATE POLICY "Users submit content reports" ON public.content_reports FOR INSERT
    WITH CHECK (auth.uid() = reporter_id);
