#!/usr/bin/env python3
"""Fail if server-safe source references client-only Minecraft classes.

`common/` and `fabric/src/main/java` are loaded on dedicated servers, where the
`net.minecraft.client` package does not exist. A single reference there is a
guaranteed NoClassDefFoundError for every server operator, and it compiles
cleanly, so nothing else catches it.

A plain `grep net.minecraft.client` is not good enough: it also matches the word
inside javadoc and comments, where it is not a reference at all but a perfectly
reasonable thing to write about. This strips comments and string literals first,
so only real code counts.

    python3 scripts/check-side-safety.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# Source roots that must survive on a dedicated server.
#
# NOT included: fabric/src/client/java (a client-only source set by design) and
# neoforge/.../client (guarded by @Mod(dist = Dist.CLIENT)).
SERVER_SAFE_ROOTS = [
    "common/src/main/java",
    "fabric/src/main/java",
]

FORBIDDEN = re.compile(r"\bnet\.minecraft\.client\b")

BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.DOTALL)
LINE_COMMENT = re.compile(r"//[^\n]*")
STRING_LITERAL = re.compile(r'"(?:\\.|[^"\\])*"')


def strip_non_code(source: str) -> str:
    """Blank out comments and string literals, preserving line numbering."""
    def blank(match: re.Match[str]) -> str:
        # Keep the newlines so reported line numbers stay accurate.
        return re.sub(r"[^\n]", " ", match.group(0))

    source = BLOCK_COMMENT.sub(blank, source)
    source = LINE_COMMENT.sub(blank, source)
    source = STRING_LITERAL.sub(blank, source)
    return source


def main() -> int:
    violations: list[str] = []
    scanned = 0

    for root in SERVER_SAFE_ROOTS:
        base = ROOT / root
        if not base.is_dir():
            continue
        for java_file in sorted(base.rglob("*.java")):
            scanned += 1
            code = strip_non_code(java_file.read_text(encoding="utf-8"))
            for lineno, line in enumerate(code.splitlines(), start=1):
                if FORBIDDEN.search(line):
                    rel = java_file.relative_to(ROOT)
                    violations.append(f"{rel}:{lineno}: {line.strip()}")

    print(f"Scanned {scanned} server-safe source files in: {', '.join(SERVER_SAFE_ROOTS)}")

    if violations:
        print(
            "\nThese files run on dedicated servers but reference client-only "
            "classes:\n", file=sys.stderr,
        )
        for violation in violations:
            print(f"  {violation}", file=sys.stderr)
        print(
            "\nMove this code to fabric/src/client/java, or to a "
            "@Mod(dist = Dist.CLIENT) class on NeoForge.\n"
            "See docs/02-sides-client-vs-server.md",
            file=sys.stderr,
        )
        return 1

    print("OK: no client-only references in server-safe code.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
