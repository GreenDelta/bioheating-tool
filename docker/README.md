# Docker setup

For the Docker setup, we have two images that we need to build:

+ `bioheating-db` for the BioHeating Tool database
+ `bioheating-app` for the BioHeating Tool application


## Building the database image

Use the `db.Dockerfile` to build the database image like this:

```bash
# copy the schema file to the docker directory
cd docker
cp ../server/schema.sql ./app/schema.sql

# build the image
docker build -t bioheating-db . -f db.Dockerfile

# run it with required environment variables
docker run --rm -d -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=bioheating \
  -e POSTGRES_DB=bioheating \
  --name bioheating-db bioheating-db

# or interactively
docker run --rm -it -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=bioheating \
  -e POSTGRES_DB=bioheating \
  --name bioheating-db bioheating-db

# and of course, map the data outside of the container ...
docker run --rm -it \
  -v ./data/bioheating-db:/var/lib/postgresql/data \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=bioheating \
  -e POSTGRES_DB=bioheating \
  -e PGDATA=/var/lib/postgresql/data \
  -p 5432:5432 \
  --name bioheating-db \
  bioheating-db
```

**Note:** The database requires environment variables to be set when running the container. These are automatically configured when using docker-compose (see below).

A running database container is also useful when developing the application.


## Building the application image

For the application image, use the `app.Dockerfile`. **Make sure** to run the
application build first and copy the required files to the `docker` folder, before building the
image:

```bash

# first build the application; this will produce the target/bioheating-tool.jar file
mvn clean package -DskipTests
rm docker/bioheating-tool.jar
cp server/target/bioheating-tool-*.jar docker/bioheating-tool.jar

# copy the static files (built UI)
cp -r server/static docker/

cd docker

# build the image
docker build -t bioheating-app . -f app.Dockerfile

# run it (with upload volume mapping)
docker run --rm -d -p 3000:3000 \
  -v ./upload-data:/app/uploads \
  --name bioheating-app bioheating-app

# or interactively
docker run --rm -it -p 3000:3000 \
  -v ./upload-data:/app/uploads \
  --name bioheating-app bioheating-app
```


## Deployment

**Recommended:** Use docker-compose to run both services with proper configuration:

```bash
docker compose up

# add the -d flag to run it in detached mode
docker compose up -d
```

The docker-compose.yml file automatically configures:
- Required environment variables for the database
- Volume mapping for file uploads (`./upload-data:/app/uploads`)
- Proper service dependencies and health checks

---

The current (manual) deployment strategy is to build the images locally, export
them to a tarball, and then import them on the server. This is done like this:

```bash
docker save bioheating-db > bioheating-db.tar
docker save bioheating-app > bioheating-app.tar

# for faster transfer to the server, you want to compress the tarballs
gzip bioheating-db.tar  # this will create bioheating-db.tar.gz
gzip bioheating-app.tar  # this will create bioheating-app.tar.gz
```

Then copy the tarballs to the server and import them:

```bash
# decompress the tarballs first
gunzip bioheating-db.tar.gz
gunzip bioheating-app.tar.gz

# then load them
docker load < bioheating-db.tar
docker load < bioheating-app.tar
```

(Note that we plan to use a proper CI/CD pipeline in the future.)

There is a `docker-compose.yml` file that can be used to run the application
and the database together with all required environment variables pre-configured.

```bash
docker compose up

# add the -d flag to run it in detached mode
docker compose up -d
```
