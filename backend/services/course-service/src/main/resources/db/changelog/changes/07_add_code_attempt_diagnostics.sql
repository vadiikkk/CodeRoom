--liquibase formatted sql

--changeset coderoom:course-07
alter table code_submission_attempts
    add column if not exists exit_code int null,
    add column if not exists tests_passed int null,
    add column if not exists tests_total int null,
    add column if not exists scoring_mode varchar(30) null;
