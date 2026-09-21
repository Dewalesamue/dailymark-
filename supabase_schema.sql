-- ====================================================================
-- SUPABASE AUTH + STORAGE SCHEMA & RLS MIGRATION SPEC
-- Run this in your Supabase SQL Editor (Dashboard > SQL Editor > New query)
-- ====================================================================

-- 1. Create PROFILES Table (Scoped to auth.uid())
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT,
    email TEXT,
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Create DAYS Table
CREATE TABLE IF NOT EXISTS public.days (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    custom_name TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_user_day UNIQUE (user_id, date)
);

-- 3. Create PHOTOS Table
CREATE TABLE IF NOT EXISTS public.photos (
    id TEXT PRIMARY KEY,
    day_id TEXT NOT NULL REFERENCES public.days(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    storage_path TEXT NOT NULL,
    media_type TEXT NOT NULL DEFAULT 'photo',
    caption TEXT,
    mood TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Enable Row Level Security (RLS) ON ALL THREE TABLES (MANDATORY)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.days ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.photos ENABLE ROW LEVEL SECURITY;

-- 5. RLS Policies for PROFILES
-- user can SELECT/UPDATE only their own row (id = auth.uid())
DROP POLICY IF EXISTS "Users can read own profile" ON public.profiles;
CREATE POLICY "Users can read own profile"
    ON public.profiles FOR SELECT
    USING (id = auth.uid());

DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    USING (id = auth.uid());

DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
CREATE POLICY "Users can insert own profile"
    ON public.profiles FOR INSERT
    WITH CHECK (id = auth.uid());

-- 6. RLS Policies for DAYS
-- user can SELECT/INSERT/UPDATE/DELETE only rows where user_id = auth.uid()
DROP POLICY IF EXISTS "Users can manage own days" ON public.days;
CREATE POLICY "Users can manage own days"
    ON public.days FOR ALL
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

-- 7. RLS Policies for PHOTOS
-- user can SELECT/INSERT/UPDATE/DELETE only rows where user_id = auth.uid()
DROP POLICY IF EXISTS "Users can manage own photos" ON public.photos;
CREATE POLICY "Users can manage own photos"
    ON public.photos FOR ALL
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

-- 8. Auto-create user's profile row on first sign-in via Postgres trigger on auth.users insert
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, name, email, avatar_url, created_at)
    VALUES (
        NEW.id,
        COALESCE(
            NEW.raw_user_meta_data->>'full_name',
            NEW.raw_user_meta_data->>'name',
            split_part(NEW.email, '@', 1)
        ),
        NEW.email,
        COALESCE(
            NEW.raw_user_meta_data->>'avatar_url',
            NEW.raw_user_meta_data->>'picture',
            ''
        ),
        NOW()
    )
    ON CONFLICT (id) DO UPDATE SET
        name = COALESCE(EXCLUDED.name, public.profiles.name),
        email = COALESCE(EXCLUDED.email, public.profiles.email),
        avatar_url = COALESCE(EXCLUDED.avatar_url, public.profiles.avatar_url),
        updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT OR UPDATE ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 9. Create Private Storage Bucket 'memories' (not public)
INSERT INTO storage.buckets (id, name, public)
VALUES ('memories', 'memories', false)
ON CONFLICT (id) DO UPDATE SET public = false;

-- 10. Storage RLS Policies for 'memories' Bucket
-- Storage path convention: {user_id}/{day_id}/{file_id}.{ext}
-- Storage policies: a user can only read/write files whose path starts with their own auth.uid()
DROP POLICY IF EXISTS "Users can upload own memories" ON storage.objects;
CREATE POLICY "Users can upload own memories"
    ON storage.objects FOR INSERT
    WITH CHECK (
        bucket_id = 'memories' AND
        (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS "Users can read own memories" ON storage.objects;
CREATE POLICY "Users can read own memories"
    ON storage.objects FOR SELECT
    USING (
        bucket_id = 'memories' AND
        (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS "Users can update own memories" ON storage.objects;
CREATE POLICY "Users can update own memories"
    ON storage.objects FOR UPDATE
    USING (
        bucket_id = 'memories' AND
        (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS "Users can delete own memories" ON storage.objects;
CREATE POLICY "Users can delete own memories"
    ON storage.objects FOR DELETE
    USING (
        bucket_id = 'memories' AND
        (storage.foldername(name))[1] = auth.uid()::text
    );
