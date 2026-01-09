# Docker setup

For the Docker setup, we have two images that we need to build:

+ `bioheating-db` - the database image
+ `bioheating-app` - the image with the application server


## Building all images

To build both the database and application images and export them as compressed tarballs, use the `docker-all` command:

```bash
dart run docker/build.dart docker-all
```

This command will:
+  Check that no containers (`bioheating-db`, `bioheating-app`) are currently running
+  Delete any existing images with these names
+ Build both images
+ Export them to `docker/images/` as `.tar.gz` files

The exported files can then be transferred to a server for deployment.


## Building the database image

Build the `bioheating-db` image via the `docker/build.dart` script (see the `docker-db` command in the script):

```bash
dart run docker/build.dart docker-db
```

For running the database image, use `docker-compose.db-dev.yaml` to run just the database container (it contains additional details in its comments).


## Building the application image

To build only the application image, use the `docker-app` command:

```bash
dart run docker/build.dart docker-app
```

This will build the UI, the server, and then the Docker image `bioheating-app`.
Use docker-compose to run both services with proper configuration. Run these commands from the `docker/` directory:

```bash
cd docker

# Start everything
docker compose up

# OR: add the -d flag to run it in detached mode
docker compose up -d

# To stop (if run in detached mode)
docker compose down
```

The `docker-compose.yml` file automatically configures:

- Required environment variables for the database
- Volume mapping for PostgreSQL data (`./data/bioheating-db`)
- Volume mapping for file uploads (`./data/uploads`)
- Proper service dependencies and health checks
- `network_mode: host` to allow the containers to communicate easily


### External Deployment (Server)

To deploy to a remote server run `dart run docker/build.dart docker-all` locally.
Copy these files** to your server:

+ `docker/images/bioheating-db.tar.gz`
+ `docker/images/bioheating-app.tar.gz`
+ `docker/docker-compose.yaml`
+ `docker/init-db.sql`

Then, on the server:

```bash
# Decompress and load the images
gunzip *.tar.gz

docker load < bioheating-db.tar
docker load < bioheating-app.tar

# Start the application
docker compose up -d
```

When you run `docker compose up`, the `data/bioheating-db` and `data/uploads` folders will be created automatically if they don't exist. When uploading a new version, you do not need to delete the `data/` folder. Keeping it ensures your database and uploaded files persist across updates. Only delete the `data/` folder if you want to wipe the database and start with a clean installation. Note that you may need `sudo` to delete these folders as Docker creates them with root permissions.
