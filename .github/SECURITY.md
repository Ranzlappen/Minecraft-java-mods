# Security policy

## Reporting a vulnerability

**Do not open a public issue for a security problem.**

Use [GitHub's private vulnerability reporting](https://github.com/Ranzlappen/Minecraft-java-mods/security/advisories/new)
(Security tab → Report a vulnerability). You will get an acknowledgement within
a few days.

## What counts as a vulnerability in a Minecraft mod

The interesting threat surface for a server-side mod is anything a player can
reach without operator permission:

- **Command injection or permission bypass** — a command that should require
  permission level 2 being usable by anyone.
- **Malicious network payloads** — a crafted packet that crashes the server,
  reads arbitrary files, or executes code. Custom payload handlers are the main
  attack surface a mod adds.
- **Resource exhaustion** — an action a normal player can repeat to lock up the
  server thread or fill the disk.
- **Data exposure** — leaking another player's data, server paths, or tokens
  into chat or logs.
- **Unsafe deserialisation** — NBT or JSON from an untrusted source parsed into
  arbitrary types.

Not vulnerabilities: griefing that vanilla also allows, balance problems, and
crashes only reachable by an operator or by another mod.

## Supported versions

Security fixes go to the latest release on each actively maintained Minecraft
version — `main`, plus any `mc/*` maintenance branch listed in the README.
Older versions are not patched.

## Handling untrusted input

If you are contributing, the rule is that **the server trusts nothing from a
client**. See [docs/08-networking.md](../docs/08-networking.md#6-rules-that-save-you-pain).
Validate every field of every inbound payload: range, permission, and whether
the acting player is actually able to do the thing they are claiming.

## Supply chain

- Every GitHub Action is pinned to a full commit SHA, enforced by
  `.github/workflows/repo-sanitation.yml`.
- The Gradle distribution is pinned by SHA-256 in `gradle-wrapper.properties`.
- `dependency-review.yml` blocks pull requests introducing high-severity CVEs.
- CodeQL and Gitleaks run on every push and weekly.
