#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
OUT_DIR="${ROOT_DIR}/build/neo4lite-audit"
KEEP_FILE="${ROOT_DIR}/build/neo4lite-runtime-keep.txt"
REACHABLE_OUT="${OUT_DIR}/runtime-reachable.txt"
UNREACHABLE_OUT="${OUT_DIR}/runtime-unreachable-interpreted.txt"

mkdir -p "${OUT_DIR}" "${ROOT_DIR}/build"

if [[ ! -f "${KEEP_FILE}" ]]; then
  cat > "${KEEP_FILE}" <<'KEEP'
# One entry per line. Empty lines and lines starting with '#' are ignored.
# Supported formats:
# 1) Runtime-interpreted file path under src/main/scala/org/neo4j/cypher/internal/runtime/interpreted
# 2) FQCN, for example: org.neo4j.cypher.internal.runtime.interpreted.pipes.Pipe
# 3) Simple class/file stem, for example: Pipe
KEEP
fi

python3 - "${ROOT_DIR}" "${KEEP_FILE}" "${REACHABLE_OUT}" "${UNREACHABLE_OUT}" <<'PY'
import re
import sys
from pathlib import Path
from collections import defaultdict, deque

root = Path(sys.argv[1])
keep_file = Path(sys.argv[2])
reachable_out = Path(sys.argv[3])
unreachable_out = Path(sys.argv[4])

interpreted_root = root / "src/main/scala/org/neo4j/cypher/internal/runtime/interpreted"

if not interpreted_root.exists():
    print(f"[neo4j-lite] missing interpreted runtime tree: {interpreted_root}", file=sys.stderr)
    sys.exit(1)

all_files = sorted(p for p in interpreted_root.rglob("*.scala"))
if not all_files:
    print("[neo4j-lite] no interpreted runtime scala files found", file=sys.stderr)
    sys.exit(1)

by_stem = defaultdict(set)
by_rel_path = {}
for p in all_files:
    by_stem[p.stem].add(p)
    by_rel_path[p.relative_to(root).as_posix()] = p

fqcn_pattern = re.compile(
    r"org\.neo4j\.cypher\.internal\.runtime\.interpreted(?:\.[A-Za-z_][A-Za-z0-9_]*)+"
)
upper_identifier_pattern = re.compile(r"\b[A-Z][A-Za-z0-9_]*\b")

def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8", errors="ignore")
    except OSError:
        return ""

def add_by_symbol(symbol: str, target_set: set):
    stem = symbol.split(".")[-1]
    for p in by_stem.get(stem, ()):
        target_set.add(p)

# Seed reachability from non-interpreted sources (main runtime entry path).
seed_files = set()
source_roots = [root / "src/main/scala", root / "src/main/java"]
for source_root in source_roots:
    if not source_root.exists():
        continue
    for src in source_root.rglob("*"):
        if not src.is_file():
            continue
        if src.suffix not in {".scala", ".java"}:
            continue
        if interpreted_root in src.parents:
            continue
        text = read_text(src)
        for token in fqcn_pattern.findall(text):
            add_by_symbol(token, seed_files)

# Add user-provided keep entries as protected-reachable.
if keep_file.exists():
    for raw in keep_file.read_text(encoding="utf-8", errors="ignore").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line in by_rel_path:
            seed_files.add(by_rel_path[line])
            continue
        maybe_path = (root / line).resolve()
        if maybe_path.exists() and maybe_path in all_files:
            seed_files.add(maybe_path)
            continue
        if line.startswith("org.neo4j.cypher.internal.runtime.interpreted."):
            add_by_symbol(line, seed_files)
            continue
        if line.endswith(".scala"):
            rel = line.lstrip("./")
            if rel in by_rel_path:
                seed_files.add(by_rel_path[rel])
                continue
        add_by_symbol(line, seed_files)

# Build transitive closure inside interpreted runtime.
reachable = set(seed_files)
queue = deque(seed_files)
while queue:
    cur = queue.popleft()
    text = read_text(cur)
    discovered = set()

    # FQCN imports/usages.
    for token in fqcn_pattern.findall(text):
        add_by_symbol(token, discovered)

    # Conservative fallback: same-file references to known interpreted type names.
    for token in upper_identifier_pattern.findall(text):
        for p in by_stem.get(token, ()):
            discovered.add(p)

    for nxt in discovered:
        if nxt not in reachable:
            reachable.add(nxt)
            queue.append(nxt)

all_set = set(all_files)
unreachable = sorted(all_set - reachable)
reachable = sorted(reachable)

reachable_out.parent.mkdir(parents=True, exist_ok=True)
reachable_out.write_text(
    "\n".join(p.relative_to(root).as_posix() for p in reachable) + ("\n" if reachable else ""),
    encoding="utf-8",
)
unreachable_out.write_text(
    "\n".join(p.relative_to(root).as_posix() for p in unreachable) + ("\n" if unreachable else ""),
    encoding="utf-8",
)

print(f"[neo4j-lite] interpreted-total={len(all_files)} reachable={len(reachable)} unreachable={len(unreachable)}")
print(f"[neo4j-lite] reachable-file={reachable_out}")
print(f"[neo4j-lite] unreachable-file={unreachable_out}")
PY
