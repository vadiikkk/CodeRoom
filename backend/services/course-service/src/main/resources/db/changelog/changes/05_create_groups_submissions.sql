--liquibase formatted sql

--changeset coderoom:course-05
alter table course_assignments
    drop constraint if exists chk_course_assignments_weight_non_negative;

alter table course_assignments
    add constraint chk_course_assignments_weight_range check (weight >= 0 and weight <= 1);

create table if not exists course_groups (
    group_id uuid primary key,
    course_id uuid not null references courses(course_id) on delete cascade,
    name varchar(200) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_course_groups_course_name unique (course_id, name)
);

create index if not exists idx_course_groups_course_id on course_groups(course_id);

create table if not exists course_group_members (
    group_id uuid not null references course_groups(group_id) on delete cascade,
    course_id uuid not null references courses(course_id) on delete cascade,
    user_id uuid not null,
    created_at timestamptz not null default now(),
    primary key (group_id, user_id),
    constraint uq_course_group_members_course_user unique (course_id, user_id)
);

create index if not exists idx_course_group_members_course_id on course_group_members(course_id);
create index if not exists idx_course_group_members_user_id on course_group_members(user_id);

create table if not exists submissions (
    submission_id uuid primary key,
    course_id uuid not null references courses(course_id) on delete cascade,
    assignment_id uuid not null references course_assignments(assignment_id) on delete cascade,
    owner_type varchar(20) not null,
    owner_user_id uuid null,
    owner_group_id uuid null references course_groups(group_id),
    text_answer text null,
    status varchar(20) not null,
    score numeric(5, 2) null,
    comment text null,
    graded_by_user_id uuid null,
    graded_at timestamptz null,
    grader_type varchar(20) null,
    autograde_status varchar(30) null,
    external_check_status varchar(30) null,
    submitted_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_submissions_single_owner check (num_nonnulls(owner_user_id, owner_group_id) = 1),
    constraint chk_submissions_score_range check (score is null or (score >= 0 and score <= 100))
);

create unique index if not exists idx_submissions_assignment_owner_user
    on submissions(assignment_id, owner_user_id)
    where owner_user_id is not null;

create unique index if not exists idx_submissions_assignment_owner_group
    on submissions(assignment_id, owner_group_id)
    where owner_group_id is not null;

create index if not exists idx_submissions_course_id on submissions(course_id);
create index if not exists idx_submissions_assignment_id on submissions(assignment_id);

create table if not exists submission_members (
    submission_id uuid not null references submissions(submission_id) on delete cascade,
    user_id uuid not null,
    primary key (submission_id, user_id)
);

create index if not exists idx_submission_members_user_id on submission_members(user_id);

create table if not exists submission_attachments (
    submission_id uuid not null references submissions(submission_id) on delete cascade,
    attachment_id uuid not null unique references course_attachments(attachment_id) on delete cascade,
    sort_order int not null default 0,
    created_at timestamptz not null default now(),
    primary key (submission_id, attachment_id)
);

create index if not exists idx_submission_attachments_submission_id on submission_attachments(submission_id);
