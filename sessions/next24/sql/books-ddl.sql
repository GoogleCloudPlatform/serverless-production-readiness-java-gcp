-- noinspection SqlNoDataSourceInspectionForFile

-- noinspection SqlDialectInspectionForFile

-- setup alloy db follow these steps on codelabs https://codelabs.developers.google.com/codelabs/alloydb-ai-embedding#4

CREATE TYPE scope_type AS ENUM ('public', 'private');

-- 1. Authors Table
create TABLE authors (
    author_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    bio TEXT,
    embedding public.vector GENERATED ALWAYS AS (public.embedding('text-embedding-004'::text, bio)) STORED
);

-- 2. Books Table
create TABLE books (
    book_id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author_id INT NOT NULL,
    publication_year date,
    scope scope_type NOT NULL DEFAULT 'public',
    CONSTRAINT fk_author
        FOREIGN KEY(author_id)
        REFERENCES Authors(author_id)
);

-- 4. Pages Table
create TABLE public.pages (
    page_id SERIAL PRIMARY KEY,
    book_id INT NOT NULL,
    page_number INT NOT NULL,
    content TEXT,
    embedding public.vector GENERATED ALWAYS AS (public.embedding('text-embedding-004'::text, content)) STORED,
    CONSTRAINT fk_pages
        FOREIGN KEY(book_id)
        REFERENCES Books(book_id)
);

-- 5. BookSummaries Table
create TABLE bookSummaries (
    summary_id SERIAL PRIMARY KEY,
    book_id INT UNIQUE NOT NULL,
    summary TEXT NOT NULL,
    embedding public.vector GENERATED ALWAYS AS (public.embedding('text-embedding-004'::text, summary)) STORED,
    CONSTRAINT fk_book_summary
        FOREIGN KEY(book_id)
        REFERENCES Books(book_id)
);


CREATE INDEX idx_pages_book_id ON pages (book_id);
CREATE INDEX idx_books_author_id ON books (author_id);
CREATE INDEX idx_books_book_id ON books (book_id);
CREATE INDEX idx_pages_author_id ON authors (author_id);
--CREATE INDEX idx_hnsw_co_pages_embedding ON pages USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
