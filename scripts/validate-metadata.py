#!/usr/bin/env python3
"""Validate the mod metadata templates.

The metadata files (fabric.mod.json, neoforge.mods.toml) contain ${...}
placeholders that Gradle substitutes from gradle.properties at build time. This
script does the same substitution and then parses the result, which catches
three classes of mistake without running Gradle at all:

  1. A placeholder that has no matching property (typo, or a property renamed
     in gradle.properties but not in the template).
  2. A file that is not valid JSON/TOML after expansion.
  3. A version range written in the wrong grammar for its loader — Fabric and
     NeoForge use incompatible syntaxes and neither accepts the other's.

Run it directly, or let CI do it:  python3 scripts/validate-metadata.py
"""

from __future__ import annotations

import json
import re
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

PLACEHOLDER = re.compile(r"\$\{(\w+)\}")


def targets(mod_id: str) -> list[tuple[str, str]]:
    """Files to check. The mixin config is named after the mod, so it is derived
    from mod_id rather than hard-coded — otherwise this script would need
    renaming too, and would silently skip the file if anyone forgot."""
    return [
        ("fabric/src/main/resources/fabric.mod.json", "json"),
        (f"common/src/main/resources/{mod_id}.mixins.json", "json"),
        ("neoforge/src/main/resources/META-INF/neoforge.mods.toml", "toml"),
    ]


def load_properties(path: Path) -> dict[str, str]:
    props: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        props[key.strip()] = value.strip()
    return props


def main() -> int:
    props = load_properties(ROOT / "gradle.properties")
    failures: list[str] = []

    mod_id = props.get("mod_id", "")
    if not mod_id:
        print("FAIL gradle.properties: mod_id is not set", file=sys.stderr)
        return 1

    for rel, kind in targets(mod_id):
        path = ROOT / rel
        if not path.exists():
            # A loader subproject may legitimately have been deleted. The mixin
            # config may not: if it is missing, the name is wrong.
            if rel.endswith(".mixins.json"):
                failures.append(
                    f"{rel}: missing. The mixin config must be named after mod_id "
                    f"({mod_id!r}). Run ./scripts/rename-mod.sh rather than renaming by hand."
                )
            else:
                print(f"skip {rel} (not present)")
            continue

        raw = path.read_text(encoding="utf-8")

        missing = sorted({m for m in PLACEHOLDER.findall(raw) if m not in props})
        if missing:
            failures.append(f"{rel}: no such property in gradle.properties: {missing}")
            continue

        expanded = PLACEHOLDER.sub(lambda m: props[m.group(1)], raw)

        try:
            parsed = json.loads(expanded) if kind == "json" else tomllib.loads(expanded)
        except Exception as exc:  # noqa: BLE001 - report any parse error verbatim
            failures.append(f"{rel}: does not parse after expansion: {exc}")
            continue

        problems = check_semantics(rel, parsed)
        failures.extend(problems)
        print(f"{'FAIL' if problems else 'ok  '} {rel}")

    # Cross-file consistency: the mod id in gradle.properties must match the
    # MOD_ID constant, because annotations cannot read gradle.properties.
    failures.extend(check_mod_id_constant(props))

    if failures:
        print()
        for failure in failures:
            print(f"FAIL {failure}", file=sys.stderr)
        return 1

    print("\nAll metadata valid.")
    return 0


def check_semantics(rel: str, parsed: object) -> list[str]:
    """Loader-specific rules that a plain parse would not catch."""
    problems: list[str] = []

    if rel.endswith("fabric.mod.json"):
        assert isinstance(parsed, dict)
        mc_range = parsed.get("depends", {}).get("minecraft", "")
        # Fabric uses its own range grammar; Maven brackets are silently
        # rejected by the loader at runtime, which is a miserable way to find out.
        if mc_range.startswith("[") or mc_range.startswith("("):
            problems.append(
                f"{rel}: 'depends.minecraft' is {mc_range!r}, which is Maven range "
                "syntax. Fabric needs its own grammar (e.g. '~26.2'). Use "
                "fabric_minecraft_version_range, not minecraft_version_range."
            )
        env = parsed.get("environment")
        if env not in ("*", "client", "server"):
            problems.append(f"{rel}: 'environment' must be '*', 'client' or 'server', got {env!r}")

    if rel.endswith("neoforge.mods.toml"):
        assert isinstance(parsed, dict)
        for mod_id, deps in parsed.get("dependencies", {}).items():
            for dep in deps:
                version_range = dep.get("versionRange", "")
                # NeoForge wants Maven ranges. A Fabric-style '~26.2' parses as
                # TOML but is not a valid Maven range.
                if version_range.startswith("~") or version_range.startswith(">"):
                    problems.append(
                        f"{rel}: dependency '{dep.get('modId')}' of '{mod_id}' has "
                        f"versionRange {version_range!r}, which is Fabric grammar. "
                        "NeoForge needs a Maven range (e.g. '[26.2,26.3)')."
                    )

    return problems


def check_mod_id_constant(props: dict[str, str]) -> list[str]:
    """Constants.MOD_ID must equal gradle.properties' mod_id."""
    mod_id = props.get("mod_id")
    if not mod_id:
        return ["gradle.properties: mod_id is not set"]

    matches = list(ROOT.glob("common/src/main/java/**/Constants.java"))
    if not matches:
        return []

    source = matches[0].read_text(encoding="utf-8")
    found = re.search(r'MOD_ID\s*=\s*"([^"]+)"', source)
    if not found:
        return [f"{matches[0].relative_to(ROOT)}: could not find the MOD_ID constant"]

    if found.group(1) != mod_id:
        return [
            f"mod id mismatch: gradle.properties says {mod_id!r} but "
            f"{matches[0].relative_to(ROOT)} says {found.group(1)!r}. "
            "Run ./scripts/rename-mod.sh to change both."
        ]

    return []


if __name__ == "__main__":
    sys.exit(main())
