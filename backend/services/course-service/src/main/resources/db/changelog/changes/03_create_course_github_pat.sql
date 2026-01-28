--liquibase formatted sql

--changeset coderoom:course-03
create table if not exists course_github_pat (
    course_id uuid primary key references courses(course_id) on delete cascade,
    iv bytea not null,
    ciphertext bytea not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
