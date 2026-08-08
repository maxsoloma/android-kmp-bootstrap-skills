#!/usr/bin/env zsh
# generate.sh — generate an Android project from bootstrap-android-project templates
# Requires zsh (macOS default since Catalina; bash 3.2 has no assoc arrays).
#
# Usage:
#   generate.sh <outputDir> <PROJECT_NAME> <PACKAGE_BASE> <APP_ID> <MIN_SDK> <TARGET_SDK> <FEATURE_NAME> [optionalModule...]
#
# Arguments:
#   outputDir     : absolute or relative path where the project will be created
#   PROJECT_NAME  : PascalCase root project name (e.g. Sample)
#   PACKAGE_BASE  : dotted package base (e.g. io.sample.app)
#   APP_ID        : Android applicationId in :app (e.g. io.sample.app)
#   MIN_SDK       : android.minSdk value (e.g. 26)
#   TARGET_SDK    : android.targetSdk value (e.g. 35)
#   FEATURE_NAME  : PascalCase starter feature name (e.g. Main)
#   optionalModule: zero or more optional module names; dependencies are resolved
#                   transitively from core-optional/<m>/_manifest.yml requires: field
#
# Example:
#   generate.sh .scratch/andgen Sample io.sample.app io.sample.app 26 35 Main database billing
#
# Token mapping (7 tokens — no eighth):
#   {{PROJECT_NAME}} → PROJECT_NAME (e.g. Sample)
#   {{PACKAGE_BASE}} → PACKAGE_BASE (e.g. io.sample.app)
#   {{PACKAGE_PATH}} → derived from PACKAGE_BASE, dots→slashes (e.g. io/sample/app)
#   {{APP_ID}}       → APP_ID (e.g. io.sample.app)
#   {{MIN_SDK}}      → MIN_SDK (e.g. 26)
#   {{TARGET_SDK}}   → TARGET_SDK (e.g. 35)
#   {{FEATURE_NAME}} → FEATURE_NAME in two contexts:
#                       PascalCase (Main)  in file/class names
#                       lowercase  (main)  in package segments, dir names, gradle paths
#
# The convention-plugin prefix "app." is a fixed literal — NEVER substituted.

set -euo pipefail

# =============================================================================
# Arguments
# =============================================================================
if [[ $# -lt 7 ]]; then
    echo "Usage: generate.sh <outputDir> <PROJECT_NAME> <PACKAGE_BASE> <APP_ID> <MIN_SDK> <TARGET_SDK> <FEATURE_NAME> [optionalModule...]"
    exit 1
fi

DEST="$1"
PROJECT_NAME="$2"
PACKAGE_BASE="$3"
APP_ID="$4"
MIN_SDK="$5"
TARGET_SDK="$6"
FEATURE_NAME="$7"
shift 7
OPTIONAL_MODULES=("${@}")

# Derived
PACKAGE_PATH="${PACKAGE_BASE//.//}"          # io.sample.app → io/sample/app
FEATURE_NAME_LOWER="${FEATURE_NAME:l}"       # Main → main  (zsh :l modifier)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TEMPLATES_DIR="$SCRIPT_DIR/templates"

echo "=== bootstrap-android-project generator ==="
echo "  dest         : $DEST"
echo "  PROJECT_NAME : $PROJECT_NAME"
echo "  PACKAGE_BASE : $PACKAGE_BASE"
echo "  PACKAGE_PATH : $PACKAGE_PATH"
echo "  APP_ID       : $APP_ID"
echo "  MIN_SDK      : $MIN_SDK"
echo "  TARGET_SDK   : $TARGET_SDK"
echo "  FEATURE_NAME : $FEATURE_NAME (lower: $FEATURE_NAME_LOWER)"
echo "  optionals    : ${OPTIONAL_MODULES[*]:-none}"
echo ""

# =============================================================================
# Abort guard
# =============================================================================
if [[ -f "$DEST/settings.gradle.kts" ]]; then
    echo "ERROR: $DEST already contains settings.gradle.kts."
    echo "This project already exists. This skill only scaffolds new projects."
    exit 1
fi

mkdir -p "$DEST"

# =============================================================================
# Transitive closure of optional modules — manifest-driven
# Reads core-optional/<m>/_manifest.yml requires: field transitively.
# =============================================================================
typeset -A OPT_CLOSURE

expand_closure() {
    local module="$1"
    if [[ -n "${OPT_CLOSURE[$module]+x}" ]]; then return; fi
    OPT_CLOSURE[$module]=1
    local manifest="$TEMPLATES_DIR/core-optional/$module/_manifest.yml"
    if [[ ! -f "$manifest" ]]; then return; fi
    # Parse "requires: [a, b, c]" — extract the bracketed list, split on comma/space
    local requires_line
    requires_line=$(grep '^requires:' "$manifest" || true)
    # Strip "requires: [" ... "]"
    local inner
    inner=$(echo "$requires_line" | sed -e 's/^requires:[[:space:]]*//' -e 's/^\[//' -e 's/\]$//' -e 's/[[:space:]]//g')
    if [[ -z "$inner" ]]; then return; fi
    local dep
    IFS=',' read -rA deps <<< "$inner"
    for dep in "${deps[@]}"; do
        dep="${dep// /}"  # trim spaces
        [[ -n "$dep" ]] && expand_closure "$dep"
    done
}

for m in "${OPTIONAL_MODULES[@]}"; do
    expand_closure "$m"
done

echo "Optional closure: ${(k)OPT_CLOSURE}"
echo ""

# =============================================================================
# substitute_content: apply 7-token substitutions to STDIN → STDOUT
# FEATURE_NAME dual-case rule:
#   - "feature.{{FEATURE_NAME}}"  → "feature.<lower>"   (package segment in content)
#   - "feature/{{FEATURE_NAME}}"  → "feature/<lower>"   (path string in content)
#   - "projects.feature.{{FEATURE_NAME}}" → "projects.feature.<lower>"  (DSL accessor)
#   - remaining {{FEATURE_NAME}}  → <PascalCase>  (class/function names)
# All substitution is purely token-based (no golden literals).
# The "app." convention-plugin prefix is left untouched.
# =============================================================================
substitute_content() {
    LC_ALL=C sed \
      -e "s#projects\.feature\.{{FEATURE_NAME}}#projects.feature.${FEATURE_NAME_LOWER}#g" \
      -e "s#feature/{{FEATURE_NAME}}#feature/${FEATURE_NAME_LOWER}#g" \
      -e "s#feature\.{{FEATURE_NAME}}#feature.${FEATURE_NAME_LOWER}#g" \
      -e "s#{{FEATURE_NAME}}#${FEATURE_NAME}#g" \
      -e "s#{{PACKAGE_BASE}}#${PACKAGE_BASE}#g" \
      -e "s#{{PROJECT_NAME}}#${PROJECT_NAME}#g" \
      -e "s#{{APP_ID}}#${APP_ID}#g" \
      -e "s#{{MIN_SDK}}#${MIN_SDK}#g" \
      -e "s#{{TARGET_SDK}}#${TARGET_SDK}#g"
}

# =============================================================================
# substitute_path: apply token substitutions to a file path string.
# Returns the substituted path on stdout.
# Uses python3 for robustness against shell escaping with slashes and braces.
# =============================================================================
substitute_path() {
    python3 - "$1" <<PYEOF
import sys
p = sys.argv[1]
FEATURE_NAME       = "${FEATURE_NAME}"
FEATURE_NAME_LOWER = "${FEATURE_NAME_LOWER}"
PACKAGE_PATH       = "${PACKAGE_PATH}"
PACKAGE_BASE       = "${PACKAGE_BASE}"
PROJECT_NAME       = "${PROJECT_NAME}"
APP_ID             = "${APP_ID}"
MIN_SDK            = "${MIN_SDK}"
TARGET_SDK         = "${TARGET_SDK}"
# Order: compound forms first, then generic tokens
p = p.replace("feature/{{FEATURE_NAME}}", "feature/" + FEATURE_NAME_LOWER)
p = p.replace("{{FEATURE_NAME}}",         FEATURE_NAME)
p = p.replace("{{PACKAGE_PATH}}",         PACKAGE_PATH)
p = p.replace("{{PACKAGE_BASE}}",         PACKAGE_BASE)
p = p.replace("{{PROJECT_NAME}}",         PROJECT_NAME)
p = p.replace("{{APP_ID}}",               APP_ID)
p = p.replace("{{MIN_SDK}}",              MIN_SDK)
p = p.replace("{{TARGET_SDK}}",           TARGET_SDK)
print(p, end="")
PYEOF
}

# =============================================================================
# copy_template_tree: copy *.template files from src_dir to dest_dir,
# stripping .template suffix and substituting tokens in content AND path.
# Skips _manifest.yml (generator metadata, not project content).
# =============================================================================
copy_template_tree() {
    local src_dir="${1%/}"
    local dest_dir="$2"

    while IFS= read -r -d '' tmpl; do
        local rel="${tmpl#${src_dir}/}"
        local rel_stripped="${rel%.template}"
        local rel_final
        rel_final="$(substitute_path "$rel_stripped")"

        local dest_file="$dest_dir/$rel_final"
        mkdir -p "$(dirname "$dest_file")"
        substitute_content < "$tmpl" > "$dest_file"
    done < <(find "$src_dir" -name "*.template" -type f -print0)
}

# =============================================================================
# copy_binary: copy non-template files verbatim (gradle-wrapper.jar, xml, etc.)
# Skips _manifest.yml.
# =============================================================================
copy_binary() {
    local src_dir="${1%/}"
    local dest_dir="$2"

    while IFS= read -r -d '' f; do
        local base_name
        base_name="$(basename "$f")"
        [[ "$base_name" == "_manifest.yml" ]] && continue

        local rel="${f#${src_dir}/}"
        local rel_final
        rel_final="$(substitute_path "$rel")"
        local dest_file="$dest_dir/$rel_final"
        mkdir -p "$(dirname "$dest_file")"
        cp "$f" "$dest_file"
    done < <(find "$src_dir" -not -name "*.template" -type f -print0)
}

# =============================================================================
# Main copy operations (Android only — no iOS dirs)
# =============================================================================

echo "--- Copying _root ---"
copy_template_tree "$TEMPLATES_DIR/_root" "$DEST"
copy_binary "$TEMPLATES_DIR/_root" "$DEST"

echo "--- Copying _build-logic ---"
copy_template_tree "$TEMPLATES_DIR/_build-logic" "$DEST/build-logic"
copy_binary "$TEMPLATES_DIR/_build-logic" "$DEST/build-logic"

echo "--- Copying _app ---"
copy_template_tree "$TEMPLATES_DIR/_app" "$DEST/app"
copy_binary "$TEMPLATES_DIR/_app" "$DEST/app"

echo "--- Copying core-required ---"
for mod_dir in "$TEMPLATES_DIR/core-required"/*/; do
    mod_name="$(basename "$mod_dir")"
    echo "  core-required/$mod_name"
    copy_template_tree "$mod_dir" "$DEST/core/$mod_name"
    copy_binary "$mod_dir" "$DEST/core/$mod_name"
done

echo "--- Copying feature-main → feature/$FEATURE_NAME_LOWER ---"
copy_template_tree "$TEMPLATES_DIR/feature-main" "$DEST/feature/$FEATURE_NAME_LOWER"
copy_binary "$TEMPLATES_DIR/feature-main" "$DEST/feature/$FEATURE_NAME_LOWER"

echo "--- Copying optional modules: ${(k)OPT_CLOSURE:-none} ---"
for mod in "${(k)OPT_CLOSURE[@]}"; do
    if [[ -d "$TEMPLATES_DIR/core-optional/$mod" ]]; then
        echo "  core-optional/$mod"
        copy_template_tree "$TEMPLATES_DIR/core-optional/$mod" "$DEST/core/$mod"
        copy_binary "$TEMPLATES_DIR/core-optional/$mod" "$DEST/core/$mod"
    else
        echo "  WARNING: no template for optional module: $mod"
    fi
done

# =============================================================================
# Make gradlew executable
# =============================================================================
[[ -f "$DEST/gradlew" ]] && chmod +x "$DEST/gradlew"

# =============================================================================
# Generate settings.gradle.kts
# Scan every directory under app/, feature/, core/ that contains a build.gradle.kts
# and emit one include(...) line per module.
# The _root template ships settings.gradle.kts.template with:
#   rootProject.name = "..." and includeBuild("build-logic") already present.
# We overwrite it here with a fully-generated version.
# =============================================================================
echo "--- Generating settings.gradle.kts ---"
SETTINGS="$DEST/settings.gradle.kts"

# Start with the header from the template (already written by copy_template_tree)
# We regenerate completely from scratch to control includes precisely.
cat > "$SETTINGS" <<SETTINGS_EOF
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "${PROJECT_NAME}"

SETTINGS_EOF

# Discover all dirs with build.gradle.kts under app/, feature/, core/
# and emit include(...) lines sorted for determinism.
includes=()
for search_root in app feature core; do
    if [[ -d "$DEST/$search_root" ]]; then
        while IFS= read -r -d '' bld; do
            dir="$(dirname "$bld")"
            rel="${dir#${DEST}/}"
            # Convert path to gradle module path: replace / with :, prefix :
            module_path=":${rel//\//:}"
            includes+=("$module_path")
        done < <(find "$DEST/$search_root" -name "build.gradle.kts" -type f -print0)
    fi
done

# Sort and emit
for mp in "${(o)includes[@]}"; do
    echo "include(\"${mp}\")" >> "$SETTINGS"
done

echo "  Generated ${#includes} includes"

# =============================================================================
# Post-generation checks
# =============================================================================
echo ""
echo "=== Generation complete: $DEST ==="
echo ""
echo "--- Post-generation checks ---"

echo "  Leftover .template files..."
leftover=$(find "$DEST" -name "*.template" 2>/dev/null | wc -l | tr -d ' ')
echo "  count: $leftover"
if [[ "$leftover" -ne 0 ]]; then
    echo "  ERROR: leftover .template files:"
    find "$DEST" -name "*.template"
fi

echo "  Leftover {{ tokens in content..."
set +e
token_hits=$(grep -rn '{{' "$DEST" 2>/dev/null | grep -v 'Binary file')
set -e
if [[ -n "$token_hits" ]]; then
    token_count=$(echo "$token_hits" | wc -l | tr -d ' ')
    echo "  WARNING: $token_count lines with leftover tokens:"
    echo "$token_hits" | head -20
else
    echo "  OK: no leftover {{ tokens"
fi

echo "  Theme file name check..."
if find "$DEST" -name "${PROJECT_NAME}Theme.kt" -type f | grep -q .; then
    echo "  OK: ${PROJECT_NAME}Theme.kt exists"
else
    echo "  WARNING: ${PROJECT_NAME}Theme.kt not found (check template substitution)"
    find "$DEST" -name "*Theme.kt" -type f
fi

echo ""
echo "Optional closure: ${(k)OPT_CLOSURE:-none}"
echo "DONE."
