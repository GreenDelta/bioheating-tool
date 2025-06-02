-- drop database if exists bioheating;
-- create database bioheating;


drop table if exists tbl_users;
create table tbl_users (
    id int not null primary key,
    username varchar(50),
    password varchar(255),
    email varchar(100)
);


drop table if exists tbl_projects;
create table tbl_projects (
    id int not null primary key,
    name varchar,
    description text
);
