#!/usr/bin/env python3
"""Version and changelog helpers used by the release workflows.

Subcommands
-----------
  next-version <patch|minor|major>
        Print the next semver, derived from mod_version in gradle.properties.

  set-version <version>
        Write a new mod_version into gradle.properties.

  release-changelog <version>
        Rewrite CHANGELOG.md, turning the "## [Unreleased]" section into a
        dated "## [<version>]" section and inserting a fresh empty Unreleased
        block above it.

  extract-changelog <version>
        Print the body of the section for <version>. Used as the GitHub Release
        body and as the Modrinth/CurseForge changelog.

Everything is deliberately dependency-free so the workflows need no pip install.
"""

from __future__ import annotations

import argparse
import datetime as dt
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PROPERTIES = ROOT / "gradle.properties"
CHANGELOG = ROOT / "CHANGELOG.md"

SEMVER = re.compile(r"^(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?$")

EMPTY_UNRELEASED = """## [Unreleased]

### Added

### Changed

### Fixed

"""


def read_mod_version() -> str:
    for line in PROPERTIES.read_text(encoding="utf-8").splitlines():
        if line.strip().startswith("mod_version="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("mod_version not found in gradle.properties")


def next_version(bump: str) -> str:
    current = read_mod_version()
    match = SEMVER.match(current)
    if not match:
        raise SystemExit(f"mod_version {current!r} is not valid semver")

    major, minor, patch = (int(match.group(i)) for i in (1, 2, 3))
    prerelease = match.group(4)

    # Releasing a prerelease (0.5.0-beta.1) as stable drops the suffix and does
    # not otherwise bump — 0.5.0-beta.1 is already "before" 0.5.0.
    if prerelease and bump == "patch":
        return f"{major}.{minor}.{patch}"

    if bump == "major":
        return f"{major + 1}.0.0"
    if bump == "minor":
        return f"{major}.{minor + 1}.0"
    if bump == "patch":
        return f"{major}.{minor}.{patch + 1}"
    raise SystemExit(f"unknown bump type {bump!r}")


def set_version(version: str) -> None:
    if not SEMVER.match(version):
        raise SystemExit(f"{version!r} is not valid semver")
    text = PROPERTIES.read_text(encoding="utf-8")
    updated, count = re.subn(
        r"^mod_version=.*$", f"mod_version={version}", text, count=1, flags=re.MULTILINE
    )
    if count != 1:
        raise SystemExit("could not find a mod_version line to replace")
    PROPERTIES.write_text(updated, encoding="utf-8")
    print(f"mod_version={version}")


def release_changelog(version: str) -> None:
    text = CHANGELOG.read_text(encoding="utf-8")

    if f"## [{version}]" in text:
        raise SystemExit(f"CHANGELOG.md already has a section for {version}")

    if "## [Unreleased]" not in text:
        raise SystemExit("CHANGELOG.md has no '## [Unreleased]' section")

    today = dt.date.today().isoformat()
    updated = text.replace(
        "## [Unreleased]",
        f"{EMPTY_UNRELEASED.rstrip()}\n\n## [{version}] - {today}",
        1,
    )
    CHANGELOG.write_text(updated, encoding="utf-8")
    print(f"CHANGELOG.md: released {version} ({today})")


def extract_changelog(version: str) -> None:
    text = CHANGELOG.read_text(encoding="utf-8")

    # Grab everything between this version's heading and the next "## " heading.
    pattern = re.compile(
        rf"^## \[{re.escape(version)}\][^\n]*\n(.*?)(?=^## |\Z)",
        re.MULTILINE | re.DOTALL,
    )
    match = pattern.search(text)
    if not match:
        # Not fatal: a tag can legitimately be cut without a changelog entry,
        # and failing the release over it would be worse than a bland body.
        print(f"No changelog entry found for {version}.")
        return

    body = match.group(1).strip()
    # Drop headings that were left empty by the template.
    lines: list[str] = []
    for index, line in enumerate(body.splitlines()):
        if line.startswith("### "):
            rest = body.splitlines()[index + 1 :]
            has_content = False
            for following in rest:
                if following.startswith("### "):
                    break
                if following.strip():
                    has_content = True
                    break
            if not has_content:
                continue
        lines.append(line)

    print("\n".join(lines).strip() or f"Release {version}.")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    p = sub.add_parser("next-version")
    p.add_argument("bump", choices=["patch", "minor", "major"])

    p = sub.add_parser("set-version")
    p.add_argument("version")

    p = sub.add_parser("release-changelog")
    p.add_argument("version")

    p = sub.add_parser("extract-changelog")
    p.add_argument("version")

    args = parser.parse_args()

    if args.command == "next-version":
        print(next_version(args.bump))
    elif args.command == "set-version":
        set_version(args.version)
    elif args.command == "release-changelog":
        release_changelog(args.version)
    elif args.command == "extract-changelog":
        extract_changelog(args.version)

    return 0


if __name__ == "__main__":
    sys.exit(main())
