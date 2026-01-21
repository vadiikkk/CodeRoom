--liquibase formatted sql

--changeset coderoom:01-create-users
create table users (
  user_id uuid primary key not null,
  email varchar(255) not null,
  password_hash varchar(255) not null,
  role varchar(32) not null default 'STUDENT',
  is_root boolean not null default false,
  is_active boolean not null default true,
  created_at timestamp with time zone not null
);

alter table users
  add constraint uk_users_email unique (email);

alter table users
  add constraint ck_users_role check (role in ('STUDENT', 'ASSISTANT', 'TEACHER'));

-- Only one root user in the system
create unique index uk_users_single_root on users (is_root) where is_root;

--rollback drop table if exists users;
