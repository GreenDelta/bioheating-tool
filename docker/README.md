# Docker setup

For the Docker setup, we have two images that we need to build:

+ `bioheating-db` - the database image
+ `bioheating-app` - the image with the application server


## Building all images (recommended)

To build both the database and application images and export them as compressed tarballs, use the `docker-all` command:

```bash
dart run docker/build.dart docker-all
```

This command will:
1. Check that no containers (`bioheating-db`, `bioheating-app`) are currently running
2. Delete any existing images with these names
3. Build both images
4. Export them to `docker/images/` as `.tar.gz` files

The exported files can then be transferred to a server for deployment.


## Building the database image

Build the `bioheating-db` image via the `docker/build.dart` script (see the `docker-db` command in the script):

```bash
dart run docker/build.dart docker-db
```

For development, use `docker-compose.db-dev.yaml` to run just the database container (it contains additional details in its comments).


## Building the application image

To build only the application image, use the `docker-app` command:

```bash
dart run docker/build.dart docker-app
```

This will build the UI, the server, and then the Docker image `bioheating-app`.
To run the container after building:

```bash
cd docker

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

To transfer images to a server, use the `docker-all` command which exports compressed tarballs to `docker/images/`. Then copy them to the server and import:

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
