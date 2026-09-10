create table notifications(event_id varchar(36) primary key,user_id bigint not null,kind varchar(80) not null,message text not null,body text not null,is_read boolean not null default false,created_at timestamp not null default current_timestamp);
create index notifications_user_date on notifications(user_id,created_at desc);
