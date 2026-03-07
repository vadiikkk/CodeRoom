--liquibase formatted sql

--changeset coderoom:autograding-01
create table if not exists test_runs (
    test_run_id uuid primary key,
    attempt_id uuid not null unique,
    course_id uuid not null,
    assignment_id uuid not null,
    student_user_id uuid not null,
    language varchar(20) not null,
    repository_full_name varchar(255) not null,
    pull_request_url varchar(500) not null,
    pull_request_number int not null,
    pull_request_head_sha varchar(64) null,
    config_snapshot text not null,
    status varchar(30) not null,
    score numeric(5, 2) null,
    comment text null,
    result_summary text null,
    private_tests_object_key varchar(500) null,
    log_object_key varchar(500) null,
    queued_at timestamptz not null default now(),
    started_at timestamptz null,
    finished_at timestamptz null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_test_runs_score_range check (score is null or (score >= 0 and score <= 100))
);

create index if not exists idx_test_runs_assignment_id on test_runs(assignment_id);
create index if not exists idx_test_runs_course_id on test_runs(course_id);
