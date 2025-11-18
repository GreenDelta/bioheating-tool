import "dart:io";

String cmd(String command) {
  if (Platform.isWindows) {
    if (command == "mvn" || command == "npm") {
      return "$command.cmd";
    }
  }
  return command;
}

String nameOf(FileSystemEntity e) => e.path.split(Platform.pathSeparator).last;

String join(Directory dir, String name) =>
    "${dir.path}${Platform.pathSeparator}$name";

Future<void> copyDir(Directory source, Directory dest) async {
  await dest.create(recursive: true);
  await for (final f in source.list(recursive: false)) {
    final destPath = join(dest, nameOf(f));
    if (f is Directory) {
      final newDir = Directory(destPath);
      await copyDir(f, newDir);
    } else if (f is File) {
      await f.copy(destPath);
    }
  }
}

Future<Directory> freshDir(String path) async {
  final dir = new Directory(path);
  if (await dir.exists()) {
    await dir.delete(recursive: true);
  }
  await dir.create(recursive: true);
  return dir;
}

Future<void> run(String exec, List<String> args, Directory workDir) async {
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

Future<void> buildUserInterface(Directory uiDir) async {
  final modDir = Directory(join(uiDir, "node_modules"));
  if (!await modDir.exists()) {
    await run("npm", ["install"], uiDir);
  }
  await run("npm", ["run", "build"], uiDir);
}

Future<File> buildServer(Directory serverDir) async {
  await run("mvn", ["clean", "package", "-DskipTests=true"], serverDir);
  final isJar = (FileSystemEntity e) {
    if (e is! File) return false;
    final name = nameOf(e);
    return name.startsWith("bioheating-tool") && name.endsWith(".jar");
  };

  final jars = await Directory(
    join(serverDir, "target"),
  ).list().where(isJar).toList();

  if (jars.isEmpty) {
    print("Error: Could not find JAR file");
    exit(1);
  }
  return jars.first as File;
}

void main() async {
  print("Preparing build ...");
  final dir = Directory.current;
  final root = dir.parent;
  final appDir = await freshDir("${dir.path}/app");
  final serverDir = Directory("${root.path}/server");

  print("Building the UI ...");
  await buildUserInterface(Directory("${root.path}/ui"));
  await copyDir(
    Directory("${serverDir.path}/static"),
    Directory("${appDir.path}/static"),
  );

  // Build the server
  print("Building the server...");
  final jar = await buildServer(serverDir);
  await jar.copy("${appDir.path}/server.jar");
  await File("${serverDir.path}/schema.sql").copy("${appDir.path}/schema.sql");

  print("Build done!");
}
