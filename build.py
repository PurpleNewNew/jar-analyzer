import argparse
import os
import shutil
import subprocess
from pathlib import Path
from typing import Optional

VERSION = os.getenv("VERSION", "6.0")
PROJECT = "PROJECT: https://github.com/jar-analyzer/jar-analyzer"


def copy_file(src: Path, dst: Path) -> None:
    if not src.exists():
        raise FileNotFoundError(f"{src} not found")
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)


def copy_dir(src: Path, dst: Path) -> None:
    if not src.is_dir():
        raise FileNotFoundError(f"{src} not found")
    shutil.copytree(src, dst, dirs_exist_ok=True)


def write_text(dst: Path, content: str) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(content, encoding="utf-8")


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


def create_release_dir(os_name: str) -> Path:
    release_root = Path("release")
    release_root.mkdir(exist_ok=True)
    base = f"jar-analyzer-{VERSION}-{os_name}-21"
    return release_root / base


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


def resolve_java_bin(runtime_dir: Path, os_name: str) -> Path:
    if os_name == "windows":
        return runtime_dir / "bin" / "java.exe"
    return runtime_dir / "bin" / "java"


def has_jcef_module(java_bin: Path) -> bool:
    if not java_bin.exists():
        return False
    try:
        proc = subprocess.run(
            [str(java_bin), "--list-modules"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=20,
            check=False,
        )
    except Exception:
        return False
    if proc.returncode != 0:
        return False
    for line in proc.stdout.splitlines():
        if line.startswith("jcef@") or line == "jcef":
            return True
    return False


def copy_runtime(target_dir: Path, os_name: str, jbr_path: Path, jcef_path: Optional[Path]) -> None:
    if not jbr_path.is_dir():
        raise FileNotFoundError(f"JBR path not found: {jbr_path}")

    runtime_dir = target_dir / "jre"
    if runtime_dir.exists():
        shutil.rmtree(runtime_dir)
    copy_dir(jbr_path, runtime_dir)

    if jcef_path is not None:
        if not jcef_path.is_dir():
            raise FileNotFoundError(f"JCEF path not found: {jcef_path}")
        copy_dir(jcef_path, runtime_dir)

    java_bin = resolve_java_bin(runtime_dir, os_name)
    if not java_bin.exists():
        raise FileNotFoundError(f"java binary not found in runtime: {java_bin}")

    if not has_jcef_module(java_bin):
        raise RuntimeError(
            "runtime validation failed: jcef module missing. "
            "Use a JBR 21 runtime with JCEF, or provide --jcef overlay."
        )


def copy_start_files(target_dir: Path, os_name: str) -> None:
    if os_name == "windows":
        copy_file(Path("build") / "start.exe", target_dir / "start.exe")
        copy_file(Path("build") / "start-21.bat", target_dir / "start.bat")
    else:
        script = target_dir / "start.sh"
        copy_file(Path("build") / "start-21.sh", script)
        try:
            os.chmod(script, 0o755)
        except OSError:
            pass


def main() -> int:
    parser = argparse.ArgumentParser(description="Build release layout (JBR+JCEF, 21 only)")
    parser.add_argument("--os", required=True, choices=["windows", "linux", "macos"])
    parser.add_argument("--jbr", required=True, help="path to JBR 21 runtime root")
    parser.add_argument("--jcef", help="optional JCEF overlay root (copied into runtime root)")
    parser.add_argument("--clean", action="store_true", help="delete existing release target before build")
    args = parser.parse_args()

    os_name = args.os
    target_dir = create_release_dir(os_name)

    if args.clean and target_dir.exists():
        shutil.rmtree(target_dir)

    target_dir.mkdir(parents=True, exist_ok=True)

    core_jar = find_core_jar()
    jar_name = core_jar.name.replace("-jar-with-dependencies.jar", ".jar")

    copy_common_assets(target_dir, jar_name)
    copy_runtime(
        target_dir=target_dir,
        os_name=os_name,
        jbr_path=Path(args.jbr).expanduser().resolve(),
        jcef_path=Path(args.jcef).expanduser().resolve() if args.jcef else None,
    )
    copy_start_files(target_dir, os_name)

    print(f"[+] release ready: {target_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
