#!/usr/bin/env bash
#
# Self-test for scripts/rename-mod.sh.
#
# Copies the repo to a scratch directory, renames it, and asserts that every
# place the old name could hide has actually been updated. Run it after
# touching rename-mod.sh.
#
# It exists because the rename touches things no compiler checks: the *names*
# of META-INF/services files, their *contents*, the mixin config filename and
# its `package` field, and the asset directory. Each of those fails at runtime,
# not at build time.
#
#   ./scripts/selftest-rename.sh

set -euo pipefail

cd "$(dirname "$0")/.."
REPO="$PWD"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

NEW_ID="selftestmod"
NEW_GROUP="org.selftest.selftestmod"
NEW_PATH="${NEW_GROUP//.//}"

echo "Working in $WORK"
cp -r "$REPO/." "$WORK/"
cd "$WORK"

rm -rf .git build */build */run
git init -q
git add -A
git -c user.email=selftest@example.invalid -c user.name=selftest commit -qm init

OLD_ID="$(grep -E '^mod_id=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"
OLD_GROUP="$(grep -E '^mod_group_id=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"

echo "Renaming $OLD_ID/$OLD_GROUP -> $NEW_ID/$NEW_GROUP"
./scripts/rename-mod.sh --id "$NEW_ID" --name "Self Test Mod" \
                        --group "$NEW_GROUP" --author "Self Test" > /dev/null

FAILURES=0
check() {
    local description="$1"; shift
    if "$@"; then
        echo "  pass  $description"
    else
        echo "  FAIL  $description"
        FAILURES=$((FAILURES + 1))
    fi
}

file_contains() { [[ -f "$1" ]] && grep -q "$2" "$1"; }

echo
echo "Assertions:"

# --- Properties --------------------------------------------------------------
check "gradle.properties mod_id updated"      file_contains gradle.properties "^mod_id=$NEW_ID$"
check "gradle.properties group updated"       file_contains gradle.properties "^mod_group_id=$NEW_GROUP$"
check "gradle.properties mod_name updated"    file_contains gradle.properties "^mod_name=Self Test Mod$"
check "gradle.properties author updated"      file_contains gradle.properties "^mod_author=Self Test$"

# --- Package directories -----------------------------------------------------
for module_sourceset in "common:main" "common:test" "fabric:main" "fabric:client" "neoforge:main"; do
    module="${module_sourceset%%:*}"
    sourceset="${module_sourceset##*:}"
    check "$module/src/$sourceset package moved" \
        test -d "$module/src/$sourceset/java/$NEW_PATH"
done

# --- Java contents -----------------------------------------------------------
check "MOD_ID constant updated" \
    file_contains "common/src/main/java/$NEW_PATH/Constants.java" "MOD_ID = \"$NEW_ID\""
check "package declaration updated" \
    file_contains "common/src/main/java/$NEW_PATH/Constants.java" "^package $NEW_GROUP;"

# --- The bits that only fail at runtime --------------------------------------
# These four are the whole reason this self-test exists.
for module in fabric neoforge; do
    services="$module/src/main/resources/META-INF/services/$NEW_GROUP.platform.IPlatformHelper"
    check "$module services file renamed" test -f "$services"
    check "$module services file CONTENTS rewritten" \
        file_contains "$services" "^$NEW_GROUP\."
done

check "mixin config renamed" test -f "common/src/main/resources/$NEW_ID.mixins.json"
check "mixin config package field updated" \
    file_contains "common/src/main/resources/$NEW_ID.mixins.json" "\"$NEW_GROUP.mixin\""
check "asset directory renamed" test -d "common/src/main/resources/assets/$NEW_ID"
check "fabric.mod.json icon path updated" \
    file_contains fabric/src/main/resources/fabric.mod.json "assets/\${mod_id}/icon.png"

# --- Nothing left behind -----------------------------------------------------
echo
LEFTOVER="$(grep -rl --exclude-dir=.git --exclude-dir=build --exclude-dir=run \
    -e "$OLD_ID" -e "$OLD_GROUP" . 2>/dev/null || true)"
if [[ -n "$LEFTOVER" ]]; then
    echo "  FAIL  old name still present in:"
    echo "$LEFTOVER" | sed 's/^/          /'
    FAILURES=$((FAILURES + 1))
else
    echo "  pass  no trace of the old name anywhere"
fi

# --- Metadata still valid ----------------------------------------------------
if python3 scripts/validate-metadata.py > /dev/null 2>&1; then
    echo "  pass  metadata still validates after rename"
else
    echo "  FAIL  metadata validation failed after rename:"
    python3 scripts/validate-metadata.py 2>&1 | sed 's/^/          /'
    FAILURES=$((FAILURES + 1))
fi

echo
if [[ $FAILURES -eq 0 ]]; then
    echo "All rename self-tests passed."
    exit 0
fi
echo "$FAILURES rename self-test(s) FAILED."
exit 1
