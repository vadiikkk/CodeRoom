--liquibase formatted sql

--changeset coderoom:course-02
create table if not exists course_blocks (
    block_id uuid primary key,
    course_id uuid not null references courses(course_id) on delete cascade,
    title varchar(200) not null,
    position int not null,
    is_visible boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_course_blocks_course_id on course_blocks(course_id);

create table if not exists course_items (
    item_id uuid primary key,
    course_id uuid not null references courses(course_id) on delete cascade,
    block_id uuid null references course_blocks(block_id) on delete cascade,
    item_type varchar(20) not null,
    ref_id uuid not null,
    position int not null,
    is_visible boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_course_items_course_id on course_items(course_id);
create index if not exists idx_course_items_block_id on course_items(block_id);
