import "dart:convert";
import "dart:io";

typedef Dir = Directory;

void main(List<String> args) async {
  var root = Dir.current;
  int levels = 0;
  while (!isRoot(root) && levels < 10) {
    root = root.parent;
  }
  if (!isRoot(root)) {
    print("Error: Could not identify the project's root folder");
    exit(1);
  }

  final build = await Build.init(root);
  if (args.isEmpty) {
    await build.all();
  } else {
    switch (args.first) {
      case "docker-db":
        {
          await build
            ..withDocker()
            ..db();
        }
    }
  }

  print("Build done!");
}

bool isRoot(Dir dir) {
  final subs = ["ui", "server", "docker"];
  for (final sub in subs) {
    if (!Dir(join(dir, sub)).existsSync()) {
      return false;
    }
  }
  return true;
}

String nameOf(FileSystemEntity e) => e.path.split(Platform.pathSeparator).last;

String join(Dir dir, String name) =>
    "${dir.path}${Platform.pathSeparator}$name";

Future<void> copyDir(Directory source, Dir dest) async {
  await dest.create(recursive: true);
  await for (final f in source.list(recursive: false)) {
    final destPath = join(dest, nameOf(f));
    if (f is Dir) {
      final newDir = Dir(destPath);
      await copyDir(f, newDir);
    } else if (f is File) {
      await f.copy(destPath);
    }
  }
}

/// Runs an executable with the given arguments.
Future<String> run(String exec, List<String> args, Dir workDir) async {
  // add "cmd" extension for some commands on Windows
  final cmd = Platform.isWindows && (exec == "mvn" || exec == "npm")
      ? "$exec.cmd"
      : exec;

  print("\$ cd ${workDir.path}");
  print("\$ $cmd $args");
  final process = await Process.run(
    cmd,
    args,
    workingDirectory: workDir.path,
    stdoutEncoding: Encoding.getByName("UTF-8"),
  );
  print(process.stdout);
  if (process.exitCode != 0) {
    print(process.stderr);
    print("Command $cmd failed; exit build");
    exit(1);
  }
  return process.stdout is String ? process.stdout : "";
}

class Build {
  final Dir root;
  final Dir appDir;
  bool _withDocker = false;

  Dir get uiDir => Dir(join(root, "ui"));
  Dir get dockerDir => Dir(join(root, "docker"));
  Dir get serverDir => Dir(join(root, "server"));

  Build(this.root, this.appDir);

  /// Initializes the build with a fresh docker/app folder
  static Future<Build> init(Dir root) async {
    final dockDir = Dir(join(root, "docker"));
    final appDir = Dir(join(dockDir, "app"));
    if (await appDir.exists()) {
      await appDir.delete(recursive: true);
    }
    await appDir.create(recursive: true);
    return Build(root, appDir);
  }

  Build withDocker() {
    _withDocker = true;
    return this;
  }

  Future<void> all() async {
    await app();
    await db();
  }

  Future<void> db() async {
    await File(
      "${serverDir.path}/schema.sql",
    ).copy("${appDir.path}/schema.sql");

    if (_withDocker) {
      await DockerImage.db(dockerDir).build();
    }
  }

  Future<void> app() async {
    await _ui();
    await _server();
  }

  Future<void> _ui() async {
    print("Building the UI ...");
    final modDir = Dir(join(uiDir, "node_modules"));
    if (!await modDir.exists()) {
      await run("npm", ["install"], uiDir);
    }
    await run("npm", ["run", "build"], uiDir);
    await copyDir(
      Dir("${serverDir.path}/static"),
      Dir("${appDir.path}/static"),
    );
  }

  Future<void> _server() async {
    print("Building the server ...");
    await run("mvn", ["clean", "package", "-DskipTests=true"], serverDir);
    final isJar = (FileSystemEntity e) {
      if (e is! File) return false;
      final name = nameOf(e);
      return name.startsWith("bioheating-tool") && name.endsWith(".jar");
    };

    final jars = await Dir(
      join(serverDir, "target"),
    ).list().where(isJar).toList();

    if (jars.isEmpty) {
      print("Error: Could not find JAR file");
      exit(1);
    }

    final jar = jars.first as File;
    await jar.copy("${appDir.path}/server.jar");
  }
}

class DockerImage {
  final Dir dir;
  final String name;
  final String file;

  DockerImage(this.dir, this.name, this.file);
  DockerImage.db(this.dir)
    : this.name = "bioheating-db",
      this.file = "db.Dockerfile";
  DockerImage.app(this.dir)
    : this.name = "bioheating-app",
      this.file = "app.Dockerfile";

  build() async {
    // TODO: check that the DB is not running

    // delete the existing image if it already exist
    final existing = (await run("docker", ["images"], dir))
        .split("\n")
        .where((line) => line.trim().split(RegExp(r"\s+")).first == name)
        .firstOrNull;
    if (existing != null) {
      await run("docker", ["rmi", name], dir);
    }

    // build the image
    await run("docker", ["build", "-t", name, ".", "-f", file], dir);
  }
}
