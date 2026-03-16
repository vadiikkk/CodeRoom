--liquibase formatted sql

--changeset coderoom:course-06
create table if not exists code_assignments (
    assignment_id uuid primary key references course_assignments(assignment_id) on delete cascade,
    language varchar(20) not null,
    repository_name varchar(120) not null,
    repository_full_name varchar(255) not null,
    repository_url varchar(500) not null,
    default_branch varchar(120) not null,
    max_attempts int not null,
    private_tests_attachment_id uuid null references course_attachments(attachment_id) on delete set null,
    repository_private boolean not null default true,
    starter_config text not null,
    published_at timestamptz null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_code_assignments_max_attempts check (max_attempts > 0)
);

create index if not exists idx_code_assignments_private_tests_attachment
    on code_assignments(private_tests_attachment_id);

create table if not exists code_submission_attempts (
    attempt_id uuid primary key,
    course_id uuid not null references courses(course_id) on delete cascade,
    assignment_id uuid not null references course_assignments(assignment_id) on delete cascade,
    student_user_id uuid not null,
    attempt_number int not null,
    pull_request_url varchar(500) not null,
    pull_request_number int not null,
    pull_request_head_sha varchar(64) null,
    repository_full_name varchar(255) not null,
    config_snapshot text not null,
    private_tests_attachment_id uuid null references course_attachments(attachment_id) on delete set null,
    status varchar(30) not null,
    score numeric(5, 2) null,
    comment text null,
    result_summary text null,
    log_object_key varchar(500) null,
    queued_at timestamptz not null default now(),
    started_at timestamptz null,
    finished_at timestamptz null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_code_submission_attempts_student_number unique (assignment_id, student_user_id, attempt_number),
    constraint chk_code_submission_attempts_number check (attempt_number > 0),
    constraint chk_code_submission_attempts_score_range check (score is null or (score >= 0 and score <= 100))
);

create index if not exists idx_code_submission_attempts_assignment_id
    on code_submission_attempts(assignment_id);

create index if not exists idx_code_submission_attempts_student_user_id
    on code_submission_attempts(student_user_id);

create index if not exists idx_code_submission_attempts_course_id
    on code_submission_attempts(course_id);
