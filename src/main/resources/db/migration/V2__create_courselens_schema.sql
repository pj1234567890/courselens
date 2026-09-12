-- Non-destructive upgrade from the original Hibernate prototype.  These
-- renames preserve prototype rows while establishing the Flyway names.
DO $$
BEGIN
    IF to_regclass('public.pages') IS NOT NULL AND to_regclass('public.document_pages') IS NULL THEN
        ALTER TABLE pages RENAME TO document_pages;
    END IF;
    IF to_regclass('public.documents') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='documents' AND column_name='filename')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='documents' AND column_name='original_filename') THEN
        ALTER TABLE documents RENAME COLUMN filename TO original_filename;
    END IF;
    IF to_regclass('public.documents') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='documents' AND column_name='type')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='documents' AND column_name='document_type') THEN
        ALTER TABLE documents RENAME COLUMN type TO document_type;
    END IF;
    IF to_regclass('public.documents') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='documents' AND column_name='created_at')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='documents' AND column_name='uploaded_at') THEN
        ALTER TABLE documents RENAME COLUMN created_at TO uploaded_at;
    END IF;
    IF to_regclass('public.documents') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='documents' AND column_name='status')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='documents' AND column_name='processing_status') THEN
        ALTER TABLE documents RENAME COLUMN status TO processing_status;
    END IF;
    IF to_regclass('public.document_pages') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='document_pages' AND column_name='image_path')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='document_pages' AND column_name='source_path') THEN
        ALTER TABLE document_pages RENAME COLUMN image_path TO source_path;
    END IF;
    IF to_regclass('public.chunks') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='chunks' AND column_name='page_id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='chunks' AND column_name='document_page_id') THEN
        ALTER TABLE chunks RENAME COLUMN page_id TO document_page_id;
    END IF;
    IF to_regclass('public.chunks') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='chunks' AND column_name='text')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='chunks' AND column_name='chunk_text') THEN
        ALTER TABLE chunks RENAME COLUMN text TO chunk_text;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    original_filename VARCHAR(512) NOT NULL,
    storage_path TEXT NOT NULL,
    document_type VARCHAR(32) NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_status VARCHAR(32) NOT NULL,
    page_count INTEGER NOT NULL DEFAULT 0 CHECK (page_count >= 0),
    processing_error TEXT
);

CREATE TABLE IF NOT EXISTS document_pages (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    page_number INTEGER NOT NULL CHECK (page_number > 0),
    extracted_text TEXT,
    source_type VARCHAR(64) NOT NULL,
    source_path TEXT,
    handwritten BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_document_pages_document_page UNIQUE (document_id, page_number)
);

CREATE TABLE IF NOT EXISTS chunks (
    id UUID PRIMARY KEY,
    document_page_id UUID NOT NULL REFERENCES document_pages(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL CHECK (chunk_index >= 0),
    chunk_text TEXT NOT NULL,
    embedding vector(768),
    CONSTRAINT uq_chunks_page_index UNIQUE (document_page_id, chunk_index)
);

CREATE TABLE IF NOT EXISTS chat_sessions (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role VARCHAR(16) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE document_pages ADD COLUMN IF NOT EXISTS source_type VARCHAR(64);
UPDATE document_pages SET source_type = 'UNKNOWN' WHERE source_type IS NULL;
ALTER TABLE document_pages ALTER COLUMN source_type SET NOT NULL;

-- The prototype stored Gemini's serialized 768-value vectors as text.  This
-- converts that same column in place, retaining values rather than adding a
-- second embedding representation.
ALTER TABLE chunks ALTER COLUMN embedding TYPE vector(768) USING embedding::vector;
-- The prototype's embedding_vector was a second, inconsistent projection.
ALTER TABLE chunks DROP COLUMN IF EXISTS embedding_vector;

CREATE INDEX IF NOT EXISTS idx_document_pages_document_id ON document_pages(document_id);
CREATE INDEX IF NOT EXISTS idx_chunks_document_page_id ON chunks(document_page_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_created_at ON chat_messages(session_id, created_at);
