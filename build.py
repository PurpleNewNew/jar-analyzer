import argparse
import os
import shutil
from pathlib import Path

VERSION = os.getenv("VERSION", "5.13")
PROJECT = "PROJECT: https://github.com/jar-analyzer/jar-analyzer"


def copy_file(src: Path, dst: Path) -> bool:
    if not src.exists():
        print("[!] error: {} not found".format(src))
        return False
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)
    return True


def copy_dir(src: Path, dst: Path) -> bool:
    if not src.is_dir():
        print("[!] error: {} not found".format(src))
        return False
    shutil.copytree(src, dst, dirs_exist_ok=True)
    return True


def find_core_jar() -> Path:
    target_dir = Path("target")
    for jar in target_dir.rglob("*-jar-with-dependencies.jar"):
        return jar
    raise FileNotFoundError("core jar-with-dependencies not found in target/")


def resolve_agent_jar() -> Path:
    agent = Path("agent-jar-with-dependencies.jar")
    fallback = Path("lib") / "agent.jar"
    if agent.exists():
        fallback.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(agent, fallback)
        return fallback
    if fallback.exists():
        return fallback
    return agent


def write_text(dst: Path, content: str) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(content, encoding="utf-8")


def create_release_dirs(os_name: str):
    release_root = Path("release")
    release_root.mkdir(exist_ok=True)
    base = "jar-analyzer-{}-{}".format(VERSION, os_name)
    return {
        "system": release_root / "{}-system".format(base),
        "full": release_root / "{}-full".format(base),
        "21": release_root / "{}-21".format(base),
    }


def copy_common_assets(target_dir: Path, jar_name: str) -> None:
    lib_dir = target_dir / "lib"
    lib_dir.mkdir(parents=True, exist_ok=True)

    core_jar = find_core_jar()
    copy_file(core_jar, lib_dir / jar_name)

    agent_jar = resolve_agent_jar()
    if agent_jar.exists():
        copy_file(agent_jar, lib_dir / "agent.jar")

    copy_file(Path("LICENSE"), target_dir / "LICENSE")
    write_text(target_dir / "VERSION.txt", VERSION + "\n")
    write_text(target_dir / "ABOUT.txt", PROJECT + "\n")

    copy_dir(Path("rules"), target_dir / "rules")

    copy_file(Path("lib") / "jd-gui-1.6.6.jar", lib_dir / "jd-gui-1.6.6.jar")
    copy_file(Path("lib") / "README.md", lib_dir / "README.md")
    copy_file(Path("lib") / "LICENSE", lib_dir / "LICENSE")


def copy_tools_jar(target_dir: Path) -> None:
    lib_dir = target_dir / "lib"
    copy_file(Path("lib") / "tools.jar", lib_dir / "tools.jar")


def copy_start_files(target_dir: Path, os_name: str, flavor: str) -> None:
    if os_name == "windows":
        copy_file(Path("build") / "start.exe", target_dir / "start.exe")
        copy_file(Path("build") / "start-{}.bat".format(flavor), target_dir / "start.bat")
    else:
        script = target_dir / "start.sh"
        copy_file(Path("build") / "start-{}.sh".format(flavor), script)
        try:
            os.chmod(script, 0o755)
        except OSError:
            pass


def main() -> int:
    parser = argparse.ArgumentParser(description="Build release layout")
    parser.add_argument("--os", required=True, choices=["windows", "linux", "macos"])
    args = parser.parse_args()

    os_name = args.os
    release_dirs = create_release_dirs(os_name)

    core_jar = find_core_jar()
    jar_name = core_jar.name.replace("-jar-with-dependencies.jar", ".jar")

    for flavor, target_dir in release_dirs.items():
        target_dir.mkdir(parents=True, exist_ok=True)
        copy_common_assets(target_dir, jar_name)
        copy_start_files(target_dir, os_name, flavor)

    copy_tools_jar(release_dirs["system"])
    copy_tools_jar(release_dirs["full"])

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
