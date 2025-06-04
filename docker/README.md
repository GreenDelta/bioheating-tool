# Docker setup

For the Docker setup, we have two images that we need to build:

+ `bioheating-db` for the BioHeating Tool database
+ `bioheating-app` for the BioHeating Tool application


## Building the database image

Use the `db.Dockerfile` to build the database image like this:

```bash

# build the image
docker build -t bioheating-db . -f db.Dockerfile

# run it
docker run --rm -d -p 5432:5432 --name bioheating-db bioheating-db

# or interactively
docker run --rm -it -p 5432:5432 --name bioheating-db bioheating-db

# and of course, map the data outside of the container ...
docker run --rm -it \
  -v ./data/bioheating-db:/app/data/db \
  -e PGDATA=/app/data/db \
  -p 5432:5432 \
  --name bioheating-db \
  bioheating-db
```

A running database container is also useful when developing the application.


## Building the application image

For the application image, use the `app.Dockerfile`. **Make sure** to run the
application build first and copy it to the `docker` folder, before building the
image:

```bash

# first build the application; this will produce the target/bioheating-tool.jar file
mvn clean package -DskipTests
rm docker/bioheating-tool.jar
cp server/target/bioheating-tool-*.jar docker/bioheating-tool.jar

cd docker

# build the image
docker build -t bioheating-app . -f app.Dockerfile

# run it
docker run --rm -d -p 3000:3000 --name bioheating-app bioheating-app

# or interactively
docker run --rm -it -p 3000:3000 --name bioheating-app bioheating-app
```


## Deployment

The current (stupid) deployment strategy is to build the images locally, export
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
and the database together.

```bash
docker compose up

# add the -d flag to run it in detached mode
docker compose up -d
```
