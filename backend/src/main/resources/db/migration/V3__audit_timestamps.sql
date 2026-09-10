alter table app_users add column created_at timestamp not null default current_timestamp;
alter table categories add column created_at timestamp not null default current_timestamp;
alter table wallets add column updated_at timestamp not null default current_timestamp;
alter table budgets add column created_at timestamp not null default current_timestamp;
alter table budgets add column updated_at timestamp not null default current_timestamp;
alter table expense_transactions add column created_at timestamp not null default current_timestamp;
alter table expense_transactions add column updated_at timestamp not null default current_timestamp;
create function stamp_updated_at() returns trigger language plpgsql as $$
begin new.updated_at=current_timestamp; return new; end $$;
create trigger wallet_updated before update on wallets for each row execute function stamp_updated_at();
create trigger budget_updated before update on budgets for each row execute function stamp_updated_at();
create trigger transaction_updated before update on expense_transactions for each row execute function stamp_updated_at();
