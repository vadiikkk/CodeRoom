--liquibase formatted sql

--changeset coderoom:course-01
create table if not exists courses (
    course_id uuid primary key,
    owner_user_id uuid not null,
    title varchar(200) not null,
    description text null,
    is_visible boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists course_enrollments (
    course_id uuid not null references courses(course_id) on delete cascade,
    user_id uuid not null,
    role_in_course varchar(20) not null,
    created_at timestamptz not null default now(),
    primary key (course_id, user_id)
);

create index if not exists idx_course_enrollments_user_id on course_enrollments(user_id);
