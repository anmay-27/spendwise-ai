alter table expense_transactions add column provider_payment boolean not null default false;
create table checkout_orders (
 id varchar(36) primary key,
 user_id bigint not null references app_users(id),
 request_key varchar(36) not null,
 request_hash varchar(64) not null,
 provider_order_id varchar(100) not null unique,
 provider_payment_id varchar(100) unique,
 merchant varchar(160) not null,
 notes varchar(500) not null,
 category_id bigint not null references categories(id),
 amount_paise bigint not null check(amount_paise between 100 and 10000000),
 status varchar(20) not null default 'CREATED' check(status in ('CREATED','AUTHORIZED','FAILED','CAPTURED')),
 transaction_id bigint unique references expense_transactions(id),
 created_at timestamp not null default current_timestamp,
 updated_at timestamp not null default current_timestamp,
 unique(user_id,request_key)
);
create index checkout_user_date on checkout_orders(user_id,created_at desc);
