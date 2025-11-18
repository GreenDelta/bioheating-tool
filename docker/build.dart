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

      case "docker-db":  {
        await build
        ..withDocker
        ..db();
      }

    }
  }


  print("Build done!");
}

bool isRoot(Dir dir) {
    final subs = ["ui", "server", "docker"];
    for (final sub in subs) {
      if(!Dir(join(dir, sub)).existsSync()) {
        return false;
      }
    }
    return true;
  }


String cmd(String command) {
  if (Platform.isWindows) {
    if (command == "mvn" || command == "npm") {
      return "$command.cmd";
    }
  }
  return command;
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

Future<Dir> freshDir(String path) async {
  final dir = new Dir(path);
  if (await dir.exists()) {
    await dir.delete(recursive: true);
  }
  await dir.create(recursive: true);
  return dir;
}

Future<void> run(String exec, List<String> args, Dir workDir) async {
  final process = await Process.run(
    cmd(exec),
    args,
    workingDirectory: workDir.path,
  );
  print(process.stdout);
  if (process.exitCode != 0) {
    print(process.stderr);
    print("Command $exec failed; exit build");
    exit(1);
  }
}

class Build {
  final Dir root;
  final Dir appDir;
  bool withDocker = false;

  Dir get uiDir => Dir(join(root, "ui"));
  Dir get dockerDir => Dir(join(root, "docker"));
  Dir get serverDir => Dir(join(root, "server"));

  Build(this.root, this.appDir);

  static Future<Build> init(Dir root) async {
    final dockDir = Dir(join(root, "docker"));
    final appDir = await freshDir(join(dockDir, "app"));
    return Build(root, appDir);
  }

  Future<void> all() async {
    await app();
    await db();
  }

  Future<void> db() async {
    await File(
      "${serverDir.path}/schema.sql",
    ).copy("${appDir.path}/schema.sql");
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

