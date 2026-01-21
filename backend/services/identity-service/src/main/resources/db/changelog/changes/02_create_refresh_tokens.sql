--liquibase formatted sql

--changeset coderoom:02-create-refresh-tokens
create table refresh_tokens (
  id uuid primary key not null,
  user_id uuid not null references users(user_id) on delete cascade,
  token_hash varchar(64) not null,
  created_at timestamp with time zone not null,
  expires_at timestamp with time zone not null,
  revoked_at timestamp with time zone null
);

create unique index uk_refresh_tokens_hash on refresh_tokens(token_hash);
create index ix_refresh_tokens_user_id on refresh_tokens(user_id);

--rollback drop table if exists refresh_tokens;
