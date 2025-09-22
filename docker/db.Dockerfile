FROM postgres:latest

# Copy initialization scripts
COPY init-db.sql /docker-entrypoint-initdb.d/

# Set environment variables
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=bioheating
ENV POSTGRES_DB=bioheating

EXPOSE 5432
