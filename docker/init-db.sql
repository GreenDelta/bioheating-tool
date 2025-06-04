-- set database user and password
GRANT ALL PRIVILEGES ON DATABASE bioheating TO postgres;
ALTER USER postgres WITH PASSWORD 'bioheating';

-- enable PostGIS extensions
CREATE EXTENSION postgis;
CREATE EXTENSION postgis_raster;
