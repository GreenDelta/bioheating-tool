-- drop database if exists bioheating;
-- create database bioheating;

drop table if exists tbl_sequences cascade;
create table tbl_sequences (
  seq_name varchar(255) not null primary key,
  seq_value bigint not null
);
insert into tbl_sequences(seq_name, seq_value) values('entity_seq', 0);

drop table if exists tbl_users cascade;
create table tbl_users (
    id int not null primary key,
    username varchar not null unique,
    password varchar,
    full_name varchar,
    is_admin boolean
);

drop table if exists tbl_buildings cascade;
create table tbl_buildings (
    id int not null primary key,
    name varchar,
    coordinates bytea,
    f_map int,
    roof_type varchar,
    function varchar,
    height double precision,
    storeys int,
    ground_area double precision,
    heated_area double precision,
    volume double precision,
    country varchar,
    locality varchar,
    postal_code varchar,
    street varchar,
    street_number varchar,
    climate_zone int,
    heat_demand double precision,
    is_heated boolean
);

drop table if exists tbl_streets cascade;
create table tbl_streets (
    id int not null primary key,
    name varchar,
    coordinates bytea,
    f_map int
);

drop table if exists tbl_maps cascade;
create table tbl_maps (
    id int not null primary key,
    crs varchar
);

drop table if exists tbl_projects cascade;
create table tbl_projects (
    id int not null primary key,
    name varchar,
    description text,
    f_map int,
    f_user int
);
