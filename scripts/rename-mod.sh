#!/usr/bin/env bash
#
# Rename the template to your own mod.
#
# Renaming a mod by hand means touching gradle.properties, the Java package
# directories, every `package`/`import` line, the MOD_ID constant, the mixin
# config filename and its `package` field, and two META-INF/services files whose
# *names* are fully-qualified class names. Missing any one of them produces an
# error a long way from the cause, so this script does all of it at once.
#
# Usage:
#   ./scripts/rename-mod.sh --id coolmod \
#                           --name "Cool Mod" \
#                           --group com.yourname.coolmod \
#                           [--author "Your Name"]
#
# Run it on a clean working tree and commit the result on its own. It touches a
# lot of files and you do not want it tangled with real work.

set -euo pipefail

MOD_ID=""
MOD_NAME=""
GROUP=""
AUTHOR=""
DRY_RUN=0

usage() {
    sed -n '2,22p' "$0" | sed 's/^# \{0,1\}//'
    exit "${1:-1}"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --id)      MOD_ID="${2:?--id needs a value}"; shift 2 ;;
        --name)    MOD_NAME="${2:?--name needs a value}"; shift 2 ;;
        --group)   GROUP="${2:?--group needs a value}"; shift 2 ;;
        --author)  AUTHOR="${2:?--author needs a value}"; shift 2 ;;
        --dry-run) DRY_RUN=1; shift ;;
        -h|--help) usage 0 ;;
        *) echo "Unknown option: $1" >&2; usage 1 ;;
    esac
done

[[ -n "$MOD_ID" && -n "$MOD_NAME" && -n "$GROUP" ]] || {
    echo "error: --id, --name and --group are all required" >&2
    usage 1
}

cd "$(dirname "$0")/.."

# --- Validate ---------------------------------------------------------------
# Mod ids are used as registry namespaces and resource-pack namespaces, so
# Minecraft enforces this character set. Failing here is much kinder than
# failing at runtime with a cryptic ResourceLocation error.
[[ "$MOD_ID" =~ ^[a-z][a-z0-9_]{1,63}$ ]] || {
    echo "error: mod id '$MOD_ID' is invalid." >&2
    echo "       Must be 2-64 chars, lowercase letters/digits/underscores, starting with a letter." >&2
    exit 1
}

[[ "$GROUP" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]] || {
    echo "error: group '$GROUP' is not a valid lowercase Java package." >&2
    exit 1
}

OLD_ID="$(grep -E '^mod_id=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"
OLD_GROUP="$(grep -E '^mod_group_id=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"

[[ -n "$OLD_ID" && -n "$OLD_GROUP" ]] || {
    echo "error: could not read mod_id / mod_group_id from gradle.properties" >&2
    exit 1
}

if [[ "$OLD_ID" == "$MOD_ID" && "$OLD_GROUP" == "$GROUP" ]]; then
    echo "Nothing to do: already '$MOD_ID' / '$GROUP'."
    exit 0
fi

OLD_PATH="${OLD_GROUP//.//}"
NEW_PATH="${GROUP//.//}"

echo "Renaming:"
echo "  mod id   $OLD_ID  ->  $MOD_ID"
echo "  package  $OLD_GROUP  ->  $GROUP"
echo "  name     -> $MOD_NAME"
[[ -n "$AUTHOR" ]] && echo "  author   -> $AUTHOR"
echo

if [[ $DRY_RUN -eq 1 ]]; then
    echo "(dry run — nothing changed)"
    exit 0
fi

if [[ -n "$(git status --porcelain 2>/dev/null)" ]]; then
    echo "warning: working tree is not clean. Ctrl-C now if that was not intended."
    sleep 3
fi

# --- 1. Move package directories --------------------------------------------
# git mv where possible so history follows the move.
for module in common fabric neoforge; do
    for sourceset in main client test; do
        src="$module/src/$sourceset/java/$OLD_PATH"
        [[ -d "$src" ]] || continue
        dst="$module/src/$sourceset/java/$NEW_PATH"
        mkdir -p "$(dirname "$dst")"
        if git ls-files --error-unmatch "$src" >/dev/null 2>&1; then
            git mv "$src" "$dst"
        else
            mv "$src" "$dst"
        fi
        echo "  moved $src -> $dst"
    done
done

# Prune directories left empty by the moves.
find common fabric neoforge -type d -empty -delete 2>/dev/null || true

# --- 2. Rewrite file contents -----------------------------------------------
# Order matters: rewrite the fully-qualified group first, then the bare mod id.
# The group usually *ends* with the mod id, so doing the id first would corrupt
# the group into a half-renamed hybrid before the group pass ever sees it.
mapfile -t FILES < <(
    find . \
        -path ./.git -prune -o \
        -path ./build -prune -o \
        -path '*/build' -prune -o \
        -path '*/run' -prune -o \
        -path ./gradle/wrapper -prune -o \
        -type f \( -name '*.java' -o -name '*.json' -o -name '*.toml' \
                   -o -name '*.gradle' -o -name '*.properties' -o -name '*.md' \
                   -o -name '*.yml' -o -name '*.cfg' -o -name '*.py' -o -name '*.sh' \
                   -o -path '*/META-INF/services/*' \) -print
)
# NOTE: the META-INF/services glob is not optional. Those files have no
# extension and their *contents* are a fully-qualified class name, so missing
# them leaves a dangling reference that only fails at runtime, as a
# ServiceConfigurationError. That is precisely the failure this script exists
# to prevent, so it is covered by scripts/selftest-rename.sh.

for file in "${FILES[@]}"; do
    # Skip the wrapper properties and this script itself.
    [[ "$file" == "./gradle/wrapper/gradle-wrapper.properties" ]] && continue
    [[ "$file" == "./scripts/rename-mod.sh" ]] && continue
    sed -i "s|${OLD_GROUP}|${GROUP}|g; s|\\b${OLD_ID}\\b|${MOD_ID}|g" "$file"
done

# --- 3. Rename the services files -------------------------------------------
# Their FILENAME is a fully-qualified interface name, so the package rename has
# to move them too. This is the step everyone forgets by hand, and it fails at
# runtime with ServiceConfigurationError rather than at build time.
for module in fabric neoforge; do
    dir="$module/src/main/resources/META-INF/services"
    old="$dir/${OLD_GROUP}.platform.IPlatformHelper"
    new="$dir/${GROUP}.platform.IPlatformHelper"
    if [[ -f "$old" ]]; then
        if git ls-files --error-unmatch "$old" >/dev/null 2>&1; then
            git mv "$old" "$new"
        else
            mv "$old" "$new"
        fi
        echo "  moved $old -> $new"
    fi
done

# --- 4. Rename the mixin config ---------------------------------------------
old_mixins="common/src/main/resources/${OLD_ID}.mixins.json"
new_mixins="common/src/main/resources/${MOD_ID}.mixins.json"
if [[ -f "$old_mixins" ]]; then
    if git ls-files --error-unmatch "$old_mixins" >/dev/null 2>&1; then
        git mv "$old_mixins" "$new_mixins"
    else
        mv "$old_mixins" "$new_mixins"
    fi
    echo "  moved $old_mixins -> $new_mixins"
fi

# --- 5. Rename the asset directory ------------------------------------------
old_assets="common/src/main/resources/assets/${OLD_ID}"
new_assets="common/src/main/resources/assets/${MOD_ID}"
if [[ -d "$old_assets" ]]; then
    if git ls-files --error-unmatch "$old_assets" >/dev/null 2>&1; then
        git mv "$old_assets" "$new_assets"
    else
        mv "$old_assets" "$new_assets"
    fi
    echo "  moved $old_assets -> $new_assets"
fi

# --- 6. Fields that are prose, not identifiers ------------------------------
# Escape the replacement for sed: & and | are special on the right-hand side.
escape() { printf '%s' "$1" | sed -e 's/[&|\\]/\\&/g'; }

sed -i "s|^mod_name=.*|mod_name=$(escape "$MOD_NAME")|" gradle.properties
if [[ -n "$AUTHOR" ]]; then
    sed -i "s|^mod_author=.*|mod_author=$(escape "$AUTHOR")|" gradle.properties
fi

# --- 7. Verify --------------------------------------------------------------
echo
if command -v python3 >/dev/null 2>&1; then
    python3 scripts/validate-metadata.py
fi

leftovers="$(grep -rl --exclude-dir=.git --exclude-dir=build --exclude-dir=run \
    --exclude=rename-mod.sh -e "$OLD_ID" -e "$OLD_GROUP" . 2>/dev/null || true)"
if [[ -n "$leftovers" ]]; then
    echo
    echo "warning: the old name still appears in these files — check them by hand:"
    echo "$leftovers" | sed 's/^/  /'
fi

cat <<EOF

Done.

Next:
  1. Edit gradle.properties — description, homepage, sources, issues, licence.
  2. ./gradlew build
  3. git add -A && git commit -m "chore: rename template to $MOD_ID"
EOF
