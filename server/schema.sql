-- drop database if exists bioheating;
-- create database bioheating;

drop table if exists tbl_sequences;
create table tbl_sequences (
  seq_name varchar(255) not null primary key,
  seq_value bigint not null
);
insert into tbl_sequences(seq_name, seq_value) values('entity_seq', 0);

drop table if exists tbl_users;
create table tbl_users (
    id int not null primary key,
    username varchar not null unique,
    password varchar,
    full_name varchar,
    is_admin boolean
);


drop table if exists tbl_projects;
create table tbl_projects (
    id int not null primary key,
    name varchar,
    description text
);


