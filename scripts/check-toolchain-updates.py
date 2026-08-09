#!/usr/bin/env python3
"""Check whether newer Minecraft / Fabric / NeoForge versions are available.

Compares the versions pinned in gradle.properties against the upstream sources
of truth and prints a Markdown report. Exit code 0 means "up to date", 1 means
"something newer exists" — the workflow uses that to decide whether to open an
issue.

Sources queried (all public, no auth):
  Minecraft      piston-meta.mojang.com version manifest
  Fabric Loader  meta.fabricmc.net
  Fabric API     maven.fabricmc.net
  Fabric Loom    maven.fabricmc.net
  NeoForge       maven.neoforged.net
  NeoForm        maven.neoforged.net
  ModDevGradle   maven.neoforged.net

Run locally before starting an upgrade:
    python3 scripts/check-toolchain-updates.py
"""

from __future__ import annotations

import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TIMEOUT = 30

VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
FABRIC_META_LOADER = "https://meta.fabricmc.net/v2/versions/loader"
MAVEN = {
    "fabric_api": "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml",
    "loom": "https://maven.fabricmc.net/net/fabricmc/fabric-loom/net.fabricmc.fabric-loom.gradle.plugin/maven-metadata.xml",
    "neoforge": "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml",
    "neoform": "https://maven.neoforged.net/releases/net/neoforged/neoform/maven-metadata.xml",
    "moddev": "https://maven.neoforged.net/releases/net/neoforged/moddev/net.neoforged.moddev.gradle.plugin/maven-metadata.xml",
}

# Anything with one of these markers is a pre-release we should not suggest.
UNSTABLE = re.compile(r"alpha|beta|rc|pre|snapshot", re.IGNORECASE)


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": "mod-toolchain-check"})
    with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
        return response.read().decode("utf-8")


def maven_versions(url: str) -> list[str]:
    return re.findall(r"<version>([^<]+)</version>", fetch(url))


def load_properties() -> dict[str, str]:
    props: dict[str, str] = {}
    for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    return props


def latest_stable(versions: list[str], prefix: str = "") -> str | None:
    """Last non-prerelease entry, optionally restricted to a prefix.

    Maven metadata is in publication order, so the last match is the newest.
    """
    candidates = [
        v for v in versions
        if not UNSTABLE.search(v) and (not prefix or v.startswith(prefix))
    ]
    return candidates[-1] if candidates else None


def main() -> int:
    props = load_properties()
    current_mc = props.get("minecraft_version", "")
    rows: list[tuple[str, str, str, bool]] = []  # component, current, latest, outdated
    errors: list[str] = []

    def record(name: str, current: str, latest: str | None) -> None:
        if latest is None:
            errors.append(f"{name}: could not determine the latest version")
            return
        rows.append((name, current or "(unset)", latest, current != latest))

    # --- Minecraft -----------------------------------------------------------
    mc_latest_release = None
    try:
        manifest = json.loads(fetch(VERSION_MANIFEST))
        mc_latest_release = manifest["latest"]["release"]
        record("Minecraft", current_mc, mc_latest_release)
    except (urllib.error.URLError, KeyError, json.JSONDecodeError) as exc:
        errors.append(f"Minecraft: {exc}")

    # --- Fabric --------------------------------------------------------------
    try:
        loaders = json.loads(fetch(FABRIC_META_LOADER))
        stable = [entry["version"] for entry in loaders if entry.get("stable")]
        record("Fabric Loader", props.get("loader_version", ""), stable[0] if stable else None)
    except (urllib.error.URLError, KeyError, json.JSONDecodeError) as exc:
        errors.append(f"Fabric Loader: {exc}")

    try:
        # Fabric API versions are suffixed with the Minecraft version
        # (0.156.0+26.2), so filter to the one we actually target.
        api_versions = maven_versions(MAVEN["fabric_api"])
        matching = [v for v in api_versions if v.endswith(f"+{current_mc}")]
        record("Fabric API", props.get("fabric_api_version", ""),
               matching[-1] if matching else None)
    except urllib.error.URLError as exc:
        errors.append(f"Fabric API: {exc}")

    try:
        record("Fabric Loom", props.get("loom_version", ""),
               latest_stable(maven_versions(MAVEN["loom"])))
    except urllib.error.URLError as exc:
        errors.append(f"Fabric Loom: {exc}")

    # --- NeoForge ------------------------------------------------------------
    # NeoForge versions mirror Minecraft's (26.2.x.y for Minecraft 26.2), so
    # restrict to the line we target rather than reporting a different game
    # version as an "update".
    mc_line = ".".join(current_mc.split(".")[:2]) if current_mc else ""
    try:
        record("NeoForge", props.get("neoforge_version", ""),
               latest_stable(maven_versions(MAVEN["neoforge"]), prefix=f"{mc_line}."))
    except urllib.error.URLError as exc:
        errors.append(f"NeoForge: {exc}")

    try:
        record("NeoForm", props.get("neoform_version", ""),
               latest_stable(maven_versions(MAVEN["neoform"]), prefix=f"{current_mc}-"))
    except urllib.error.URLError as exc:
        errors.append(f"NeoForm: {exc}")

    try:
        record("ModDevGradle", props.get("moddev_version", ""),
               latest_stable(maven_versions(MAVEN["moddev"])))
    except urllib.error.URLError as exc:
        errors.append(f"ModDevGradle: {exc}")

    # --- Report --------------------------------------------------------------
    outdated = [r for r in rows if r[3]]

    print(f"Targeting Minecraft **{current_mc or '(unset)'}**\n")
    print("| Component | Pinned | Latest | |")
    print("| --- | --- | --- | --- |")
    for name, current, latest, is_outdated in rows:
        print(f"| {name} | `{current}` | `{latest}` | {'**update**' if is_outdated else 'ok'} |")

    if errors:
        print("\n> Could not check:")
        for error in errors:
            print(f"> - {error}")

    if mc_latest_release and current_mc and mc_latest_release != current_mc:
        print(
            f"\n### A new Minecraft release is available: `{mc_latest_release}`\n\n"
            "This is a **game version** change, not a dependency bump. Follow "
            "[docs/05-upgrading-minecraft-versions.md](../docs/05-upgrading-minecraft-versions.md) "
            "— read the migration primer, and expect compile errors."
        )

    if not outdated and not errors:
        print("\nEverything is up to date.")
        return 0

    if outdated:
        print("\nUpdate by editing `gradle.properties` — nothing else references these numbers.")

    return 1 if outdated else 0


if __name__ == "__main__":
    sys.exit(main())
