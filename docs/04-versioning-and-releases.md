# Versioning and releases

---

## 1. Version scheme

```
<semver>+mc<minecraft_version>

0.4.1+mc26.2
```

`mod_version` in `gradle.properties` is the semver part; the build appends the
Minecraft version automatically. The `+…` is semver *build metadata* — it does
not affect ordering, which is right: `0.4.1+mc26.2` and `0.4.1+mc1.21.1` are the
same mod release for two different games.

Encoding the game version in the filename is not cosmetic. Users have several
versions of the same mod in a downloads folder, and loaders give unhelpful errors
when the wrong one is installed.

### What counts as what

| Bump | When | Example |
| --- | --- | --- |
| **Major** (`1.0.0`) | Breaking change for dependants or for existing worlds: registry names removed, config keys renamed, API removed, save data incompatible | Renamed a block; old worlds lose it |
| **Minor** (`0.5.0`) | New features, new Minecraft version support, new config options with defaults | Added the horse-speed overlay; ported to 26.3 |
| **Patch** (`0.4.1`) | Bug fixes, performance, docs, dependency bumps with no behaviour change | Fixed a crash on empty config |

**A new Minecraft version is at least a minor bump**, even when your code did not
change. The artifact means something different now.

Pre-1.0 (`0.x.y`): treat minor as your breaking bump and patch as everything
else. Go 1.0.0 when other people depend on you.

---

## 2. Conventional Commits

Commit messages drive changelog generation and the automatic bump.

```
feat(fabric): add mounted horse speed overlay
fix(neoforge): survive a null vehicle when reading horse speed
docs(sides): explain optional payloads
chore(deps): bump NeoForge to 26.2.0.57
```

| Prefix | Bump |
| --- | --- |
| `feat:` | minor |
| `fix:`, `perf:` | patch |
| `docs:`, `chore:`, `test:`, `refactor:`, `ci:`, `build:`, `style:` | none |
| any + `!`, or a `BREAKING CHANGE:` footer | major |

Scopes used here: `command`, `config`, `network`, `mixin`, `fabric`, `neoforge`,
`common`, `build`, `ci`, `docs`, `deps`.

---

## 3. Cutting a release

### Automatic (normal path)

The **Version bump** workflow (`.github/workflows/version-bump.yml`) is run
manually from the Actions tab. Pick `patch`, `minor`, or `major`, and it:

1. Works out the new version from `mod_version`
2. Updates `gradle.properties`
3. Rewrites the `## [Unreleased]` section of `CHANGELOG.md` into a dated release
   section, generated from commits since the last tag
4. Commits, tags `v<version>`, and pushes

Pushing the tag triggers **Release** (`.github/workflows/release.yml`), which
builds both jars, creates the GitHub Release with the changelog section as the
body, and publishes to Modrinth / CurseForge if those IDs are configured.

### Manual

```bash
# 1. Bump
sed -i 's/^mod_version=.*/mod_version=0.5.0/' gradle.properties

# 2. Move Unreleased -> a dated section in CHANGELOG.md, by hand

# 3. Commit and tag
git commit -am "chore(release): 0.5.0"
git tag -a v0.5.0 -m "0.5.0"
git push origin main --follow-tags
```

The tag is what triggers the release. Pushing the commit alone does nothing.

### Pre-releases

```bash
git tag -a v0.5.0-beta.1 -m "0.5.0-beta.1"
```

A tag containing `-alpha`, `-beta`, or `-rc` marks the GitHub Release as a
prerelease and sets the Modrinth/CurseForge channel to `alpha` / `beta` instead
of `stable`. That is the `RELEASE_CHANNEL` environment variable the
`publishMods` blocks read.

---

## 4. Changelog

[Keep a Changelog](https://keepachangelog.com/) format. Every user-visible change
gets a line under `## [Unreleased]` in the same PR that makes it.

```markdown
## [Unreleased]

### Added
- Mounted-horse speed overlay.

### Fixed
- The saturation overlay no longer renders while spectating.
```

Sections: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`.

Write for players, not for yourself. "Fixed the saturation overlay showing in
spectator mode" beats "fixed a missing isSpectator() check".

---

## 5. Releasing for several Minecraft versions

Maintaining `main` (26.2) and `mc/1.21.1`:

- Tag from each branch with the game version in the tag:
  `v1.4.2+mc26.2`, `v1.4.2+mc1.21.1`
- Both can publish to the **same** Modrinth/CurseForge project — each file is
  tagged with the game versions it supports, and the site shows users the right
  one
- Keep one `CHANGELOG.md` per branch

Set `publish_game_versions` in `gradle.properties` on each branch to match what
that jar actually supports.

---

## 6. Repository secrets

Set under Settings → Secrets and variables → Actions.

| Secret | Needed for | Where to get it |
| --- | --- | --- |
| `MODRINTH_TOKEN` | Modrinth publishing | Modrinth → Settings → PATs, scope `Create versions` |
| `CURSEFORGE_TOKEN` | CurseForge publishing | <https://legacy.curseforge.com/account/api-tokens> |

`GITHUB_TOKEN` is provided automatically.

Both are optional. With `modrinth_id` / `curseforge_id` blank in
`gradle.properties`, those publish steps are skipped and the release still
produces a GitHub Release with the jars attached.

---

## 7. Release checklist

```markdown
- [ ] CHANGELOG `## [Unreleased]` describes every user-visible change
- [ ] `./gradlew build` green
- [ ] `:common:test` green
- [ ] Both dedicated servers start
- [ ] Tested with the installation shape users actually use
- [ ] README supported-versions table is current
- [ ] Version bumped by the right amount (new MC version => minor at least)
- [ ] Tag pushed; Release workflow green
- [ ] Modrinth/CurseForge pages show correct loaders, game versions, and environment flags
```
