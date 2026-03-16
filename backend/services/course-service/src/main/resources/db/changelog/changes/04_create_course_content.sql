--liquibase formatted sql

--changeset coderoom:course-04
create table if not exists course_materials (
    material_id uuid primary key,
    course_id uuid not null references courses(course_id) on delete cascade,
    item_id uuid not null unique references course_items(item_id) on delete cascade,
    title varchar(200) not null,
    description text null,
    body text null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_course_materials_course_id on course_materials(course_id);

create table if not exists course_assignments (
    assignment_id uuid primary key,
    course_id uuid not null references courses(course_id) on delete cascade,
    item_id uuid not null unique references course_items(item_id) on delete cascade,
    title varchar(200) not null,
    description text null,
    assignment_type varchar(20) not null,
    work_type varchar(20) not null,
    deadline_at timestamptz null,
    weight numeric(10, 2) not null default 1.00,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_course_assignments_weight_non_negative check (weight >= 0)
);

create index if not exists idx_course_assignments_course_id on course_assignments(course_id);

create table if not exists course_attachments (
    attachment_id uuid primary key,
    course_id uuid not null references courses(course_id) on delete cascade,
    material_id uuid null references course_materials(material_id) on delete cascade,
    assignment_id uuid null references course_assignments(assignment_id) on delete cascade,
    storage_bucket varchar(120) not null,
    object_key varchar(500) not null unique,
    original_filename varchar(255) not null,
    content_type varchar(255) null,
    file_size bigint not null,
    uploaded_by_user_id uuid not null,
    sort_order int not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_course_attachments_non_negative_size check (file_size >= 0),
    constraint chk_course_attachments_single_owner check (num_nonnulls(material_id, assignment_id) <= 1)
);

create index if not exists idx_course_attachments_course_id on course_attachments(course_id);
create index if not exists idx_course_attachments_material_id on course_attachments(material_id);
create index if not exists idx_course_attachments_assignment_id on course_attachments(assignment_id);
