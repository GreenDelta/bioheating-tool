import os
import shutil

from subprocess import call
from pathlib import Path


def main():
    # clean the last build
    app_dir = Path("./docker/app")
    if app_dir.exists():
        shutil.rmtree(app_dir)
    app_dir.mkdir()

    # build the UI
    server_dir = Path("./server")
    ui_dir = Path("./ui")
    if not (ui_dir / "node_modules").exists():
        call([cmd("nmp"), "install"], cwd=ui_dir)
    call([cmd("npm"), "run", "build"], cwd=ui_dir)
    shutil.copytree(server_dir / "static", app_dir / "static")

    # build the server
    call([cmd("mvn"), "clean", "package", "-DskipTests=true"], cwd=server_dir)
    jar = next((server_dir / "target").glob("bioheating-tool*"))
    shutil.copy2(jar, app_dir / "bioheating-tool.jar")

    # copy the current database schema to the docker folder
    shutil.copy2(server_dir / "schema.sql", app_dir / "schema.sql")

    print("Build done!")

def cmd(cmd: str) -> str:
    if os.name != "posix" and cmd in ["mvn", "npm"]:
        return cmd + ".cmd"
    return cmd


if __name__ == "__main__":
    main()
