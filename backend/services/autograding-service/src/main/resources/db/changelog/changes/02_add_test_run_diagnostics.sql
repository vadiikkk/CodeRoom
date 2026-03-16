--liquibase formatted sql

--changeset coderoom:autograding-02
alter table test_runs
    add column if not exists exit_code int null,
    add column if not exists tests_passed int null,
    add column if not exists tests_total int null,
    add column if not exists scoring_mode varchar(30) null;
