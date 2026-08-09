# What and why

<!-- One or two sentences. What changes, and what problem it solves. -->

Closes #

## Type

- [ ] Bug fix
- [ ] New feature
- [ ] Minecraft version port
- [ ] Build / CI
- [ ] Documentation only

## Sides

<!-- Delete this whole section for docs-only or CI-only changes. -->

- [ ] No `net.minecraft.client` reference was added under `common/` or
      `fabric/src/main/java`
- [ ] New client-only code is in `fabric/src/client/java` or a
      `@Mod(dist = Dist.CLIENT)` class
- [ ] If a network payload was added, it is `optional()` **or** this mod is
      deliberately a matched-pair install
- [ ] The installation shape did not change — or the README and the
      Modrinth/CurseForge environment flags were updated to match

Which shape does this mod have after the change?
<!-- A server-only / B client-only / C both required / D server + optional client -->
<!-- See docs/02-sides-client-vs-server.md#6-the-four-installation-shapes -->

## Testing

- [ ] `./gradlew build` passes
- [ ] `./gradlew :common:test` passes
- [ ] `./gradlew :fabric:runServer` starts and stays up
- [ ] `./gradlew :neoforge:runServer` starts and stays up
- [ ] Tested in game (say how, below)

<!-- What did you actually do in game? "Ran /playtime, relogged, checked it
     persisted" is useful. "Tested it" is not. -->

## Compatibility

- [ ] No registry names were renamed or removed (these break existing worlds)
- [ ] No config keys were renamed or removed without a migration
- [ ] `CHANGELOG.md` has an entry under `## [Unreleased]`

<!-- If any of the above are unchecked, say why here, and make sure the version
     bump reflects it. See docs/04-versioning-and-releases.md -->
