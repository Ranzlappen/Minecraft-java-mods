# Publishing

---

## 1. One plugin, three destinations

Both loader subprojects use
[`me.modmuss50.mod-publish-plugin`](https://github.com/modmuss50/mod-publish-plugin),
which handles Modrinth, CurseForge, and GitHub Releases from one config block.

Every destination is conditional on its ID being set in `gradle.properties`, so a
fresh clone publishes nothing and fails nothing.

```groovy
publishMods {
    file = remapJar.archiveFile          // jar.archiveFile on NeoForge
    displayName = "${mod_name} ${mod_version} (Fabric ${minecraft_version})"
    version = project.version
    changelog = providers.environmentVariable('CHANGELOG_BODY').getOrElse('...')
    type = providers.environmentVariable('RELEASE_CHANNEL').getOrElse('stable')
    modLoaders.add('fabric')

    if (modrinth_id) { modrinth { /* ... */ } }
    if (curseforge_id) { curseforge { /* ... */ } }
}
```

---

## 2. First-time setup

### Modrinth

1. Create the project at <https://modrinth.com/>.
2. Copy the **project ID** (Settings → General — this is the short ID, not the
   URL slug) into `modrinth_id` in `gradle.properties`.
3. Create a PAT at Modrinth → Settings → PATs with the **Create versions** scope.
4. Add it as the `MODRINTH_TOKEN` repository secret.

### CurseForge

1. Create the project at <https://legacy.curseforge.com/minecraft/mc-mods>.
2. Copy the numeric **Project ID** into `curseforge_id`.
3. Generate a token at <https://legacy.curseforge.com/account/api-tokens>.
4. Add it as the `CURSEFORGE_TOKEN` repository secret.

CurseForge requires manual approval for a new project's first file. Expect a
delay the first time.

### GitHub Releases

Nothing to configure. The release workflow attaches both jars using the
automatic `GITHUB_TOKEN`.

---

## 3. Set the environment flags correctly

Both sites ask which sides your mod supports. **Get this right** — it is what
users filter on, and what modpack authors rely on.

Map from your [installation shape](./02-sides-client-vs-server.md#6-the-four-installation-shapes):

| Shape | Client | Server |
| --- | --- | --- |
| A — server-only | Unsupported (or Optional if it works in singleplayer) | Required |
| B — client-only | Required | Unsupported |
| C — both required | Required | Required |
| D — server + optional client | **Optional** | Required |

This template ships Shape A-with-`"environment": "*"`: the mod runs in
singleplayer too, so **Client: Optional, Server: Required** is the honest
setting.

Say the same thing in the first paragraph of your README and your project
description. "Do I need this on my client?" is the most-asked question on every
mod page.

---

## 4. Releasing

Tag-driven. Push a `v*` tag and `.github/workflows/release.yml` does the rest:

1. Builds both jars
2. Extracts the matching section from `CHANGELOG.md` into `CHANGELOG_BODY`
3. Creates the GitHub Release with both jars attached
4. Publishes to Modrinth and CurseForge if configured

Channel comes from the tag: `-alpha`, `-beta`, `-rc` in the tag name set the
release to that channel and mark the GitHub Release as a prerelease.

Full walkthrough: [`04-versioning-and-releases.md`](./04-versioning-and-releases.md).

---

## 5. Declaring dependencies on the sites

The Fabric block declares Fabric API as required:

```groovy
requires { slug = 'fabric-api' }
```

Add others as needed — `optional { slug = '...' }`, `incompatible { slug = '...' }`.
Modpack authors and launchers use this to resolve installs automatically, so
keeping it accurate saves real support load.

---

## 6. A good mod page

- **Line one** says what it does and which side to install it on.
- Screenshots or a short clip. Text-only pages get skipped.
- A "Requirements" section naming the loader, Minecraft versions, and whether
  clients need it.
- A link to your issue tracker.
- The licence — people check before putting a mod in a public pack.

---

## 7. Publishing as a library

If other mods will `implementation` your jar, publish to a Maven repo as well.
The root `build.gradle` already configures `maven-publish`, defaulting to
`build/repo` so a local `./gradlew publish` works with no credentials.

Point it at a real repository when you are ready:

```groovy
publishing {
    repositories {
        maven {
            name = 'GitHubPackages'
            url = 'https://maven.pkg.github.com/Ranzlappen/Minecraft-java-mods'
            credentials {
                username = System.getenv('GITHUB_ACTOR')
                password = System.getenv('GITHUB_TOKEN')
            }
        }
    }
}
```

Also publish the `-sources` jar (already configured) — consumers will want it.
