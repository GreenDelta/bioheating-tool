FROM postgres:latest

# copy initialization scripts
COPY init-db.sql /docker-entrypoint-initdb.d/01-init-db.sql
COPY app/schema.sql /docker-entrypoint-initdb.d/02-schema.sql

EXPOSE 5432
