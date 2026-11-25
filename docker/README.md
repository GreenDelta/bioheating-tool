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
- Volume mapping for PostgreSQL data (`./data/db`)
- Volume mapping for the application data directory (`./data/app`), which
  contains the prediction model (`model/`) and the uploads (`uploads/`)
- Proper service dependencies and health checks
- `network_mode: host` to allow the containers to communicate easily


### External Deployment (Server)

To deploy to a remote server run `dart run docker/build.dart docker-all` locally.
This requires a trained model in `server/app/model` (see
`server/model-training/README.md`). Copy these files to your server:

+ `docker/images/bioheating-db.tar.gz`
+ `docker/images/bioheating-app.tar.gz`
+ `docker/images/model.tar.gz`
+ `docker/docker-compose.yaml`

Then, on the server:

```bash
# Decompress and load the images
gunzip bioheating-db.tar.gz bioheating-app.tar.gz

docker load < bioheating-db.tar
docker load < bioheating-app.tar

# Install the prediction model into data/app/model; this replaces an existing
# model including its config.json
sudo mkdir -p data/app
sudo tar -xzf model.tar.gz -C data/app

# Start the application
docker compose up -d
```

The application does not start when the prediction model is missing in
`data/app/model`; check the logs with `docker logs bioheating-app`. The model
consists of the files `demand-model.ubj`, `peak-model.ubj`, and `config.json`.
The `config.json` contains the corrections that are applied to the model
outputs and can be edited on the server (restart the app afterwards with
`docker compose restart bioheating-app`).

When you run `docker compose up`, the `data/db` and `data/app/uploads` folders will be created automatically if they don't exist. When uploading a new version, you do not need to delete the `data/` folder. Keeping it ensures your database and uploaded files persist across updates. Only delete the `data/` folder if you want to wipe the database and start with a clean installation. Note that you may need `sudo` to delete these folders as Docker creates them with root permissions.

### Parallel tasks and server sizing

Project imports, data updates, and calculations run as background tasks. Two
settings in `docker-compose.yaml` control the CPU load:

* `BIOHEATING_TASKS_MAX_PARALLEL`: the maximum number of tasks that run at the
  same time (default `2`); further tasks wait in a queue
* `BIOHEATING_CALCULATION_THREADS`: the threads of a single calculation
  (default `0` = about the half of the available processors)

The memory is set via `mem_limit` and `-Xmx` in `JAVA_TOOL_OPTIONS`. For each
task, the application logs the waiting time in the queue, the duration, and
the used heap memory after the task:

```bash
docker logs bioheating-app 2>&1 | grep -E "(Started|Finished) .* task"
```

To size a server, run representative projects (e.g. with 100, 1000, and 5000
buildings) one after another and in parallel, and compare these values with
the CPU and memory usage of `docker stats`. A rough estimate is then:

* memory: base heap + parallel tasks × heap peak of a large task
* CPU cores: parallel calculations × threads per calculation
