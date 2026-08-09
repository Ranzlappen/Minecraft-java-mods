#!/usr/bin/env python3
"""Check that relative links between Markdown files resolve, anchors included.

The external link checker in CI (lychee) handles http(s) URLs. This handles the
part it cannot: cross-references between the docs, and `#anchor` fragments that
point at headings which may have been renamed.

Anchor slugs follow GitHub's algorithm: lowercase, drop everything that is not
alphanumeric / space / hyphen, then replace each space with a hyphen. Note that
spaces are NOT collapsed first, so "Step 4 — Bump" becomes "step-4--bump" with
two hyphens, because the em dash is removed and leaves two spaces behind.

    python3 scripts/check-doc-links.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

LINK = re.compile(r"\[([^\]]*)\]\(([^)]+)\)")
HEADING = re.compile(r"^#{1,6}\s+(.*?)\s*$", re.MULTILINE)
SKIP_DIRS = {".git", "build", "run", ".gradle"}


def slugify(heading: str) -> str:
    """Reproduce GitHub's heading-anchor algorithm."""
    text = heading.strip().lower()
    # Inline code, emphasis and links are rendered away before slugging.
    text = re.sub(r"`([^`]*)`", r"\1", text)
    text = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", text)
    text = re.sub(r"[^\w\s-]", "", text, flags=re.UNICODE)
    text = text.replace("_", "")
    return text.replace(" ", "-")


def anchors_in(path: Path) -> set[str]:
    return {slugify(h) for h in HEADING.findall(path.read_text(encoding="utf-8"))}


def main() -> int:
    problems: list[str] = []
    checked = 0

    markdown_files = [
        p for p in sorted(ROOT.rglob("*.md"))
        if not SKIP_DIRS.intersection(p.relative_to(ROOT).parts)
    ]

    anchor_cache: dict[Path, set[str]] = {}

    for md in markdown_files:
        for label, target in LINK.findall(md.read_text(encoding="utf-8")):
            if target.startswith(("http://", "https://", "mailto:")):
                continue

            rel = md.relative_to(ROOT)
            path_part, _, anchor = target.partition("#")

            if not path_part:
                # Same-file anchor.
                resolved = md
            else:
                checked += 1
                resolved = (md.parent / path_part).resolve()
                if not resolved.exists():
                    problems.append(f"{rel}: [{label}]({target}) -> no such file")
                    continue

            if anchor and resolved.suffix == ".md":
                if resolved not in anchor_cache:
                    anchor_cache[resolved] = anchors_in(resolved)
                if anchor not in anchor_cache[resolved]:
                    problems.append(
                        f"{rel}: [{label}]({target}) -> "
                        f"no heading '#{anchor}' in {resolved.name}"
                    )

    print(f"Checked {checked} relative links across {len(markdown_files)} Markdown files.")

    if problems:
        print(f"\n{len(problems)} problem(s):", file=sys.stderr)
        for problem in problems:
            print(f"  {problem}", file=sys.stderr)
        return 1

    print("All relative links and anchors resolve.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
