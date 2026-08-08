#!/usr/bin/env zsh
# generate.sh — generate a KMP project from bootstrap-kmp-project templates
# Requires zsh (macOS default since Catalina; bash 3.2 has no assoc arrays).
#
# Usage: generate.sh <outputDir> <projectName> <packageBase> <appId> <featureName> [optionalModule...]
# Example: ./generate.sh .e2e-out/demo Demo com.demo.app com.demo.app Main database billing
#
# Token values:
#   PROJECT_NAME          : e.g. Demo
#   PACKAGE_BASE          : e.g. com.demo.app
#   PACKAGE_PATH          : derived from PACKAGE_BASE (dots → slashes)
#   APP_ID                : e.g. com.demo.app
#   MIN_SDK               : 26 (fixed)
#   TARGET_SDK            : 36 (fixed)
#   IOS_DEPLOYMENT_TARGET : 15.0 (fixed)
#   FEATURE_NAME          : e.g. Main (PascalCase)
#
# FEATURE_NAME dual-case rule:
#   PascalCase (Main)  in file names / class identifiers
#   lowercase  (main)  in dir paths / package segments / gradle project paths
set -euo pipefail

DEST="$1"
PROJECT_NAME="$2"
PACKAGE_BASE="$3"
APP_ID="$4"
FEATURE_NAME="$5"
shift 5
OPTIONAL_MODULES=("${@}")

# Fixed values
MIN_SDK="26"
TARGET_SDK="36"
IOS_DEPLOYMENT_TARGET="15.0"

# Derived
PACKAGE_PATH="${PACKAGE_BASE//.//}"                             # com.demo.app → com/demo/app
FEATURE_NAME_LOWER="${FEATURE_NAME:l}"                         # Main → main (zsh lowercase)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TEMPLATES_DIR="$SCRIPT_DIR/templates"

echo "=== bootstrap-kmp-project generator ==="
echo "  dest         : $DEST"
echo "  PROJECT_NAME : $PROJECT_NAME"
echo "  PACKAGE_BASE : $PACKAGE_BASE"
echo "  PACKAGE_PATH : $PACKAGE_PATH"
echo "  APP_ID       : $APP_ID"
echo "  FEATURE_NAME : $FEATURE_NAME (lower: $FEATURE_NAME_LOWER)"
echo "  optionals    : ${OPTIONAL_MODULES[*]:-none}"
echo ""

# Abort if target already contains settings.gradle.kts
if [[ -f "$DEST/settings.gradle.kts" ]]; then
    echo "ERROR: $DEST already contains settings.gradle.kts."
    echo "This project already exists. This skill only scaffolds new projects."
    exit 1
fi

mkdir -p "$DEST"

# =============================================================================
# Transitive closure of optional modules
# =============================================================================
typeset -A OPT_CLOSURE

expand_closure() {
    local module="$1"
    if [[ -n "${OPT_CLOSURE[$module]+x}" ]]; then return; fi
    OPT_CLOSURE[$module]=1
    case "$module" in
        billing)  expand_closure analytics; expand_closure config ;;
        sync)     expand_closure database ;;
    esac
}

for m in "${OPTIONAL_MODULES[@]}"; do
    expand_closure "$m"
done

echo "Optional closure: ${(k)OPT_CLOSURE}"
echo ""

# =============================================================================
# substitute_content: apply all 8 token substitutions (+ FEATURE_NAME dual-case)
# to STDIN → STDOUT via portable BSD/GNU sed (boundary-free, longest-first)
# =============================================================================
substitute_content() {
    LC_ALL=C sed \
      -e "s#applicationId = \"com\\.acme\\.outfit\"#applicationId = \"${APP_ID}\"#g" \
      -e "s#PRODUCT_BUNDLE_IDENTIFIER = com\\.acme\\.outfit#PRODUCT_BUNDLE_IDENTIFIER = ${APP_ID}#g" \
      -e "s#minSdk = \"26\"#minSdk = \"${MIN_SDK}\"#g" \
      -e "s#targetSdk = \"36\"#targetSdk = \"${TARGET_SDK}\"#g" \
      -e "s#IPHONEOS_DEPLOYMENT_TARGET = 15\\.0#IPHONEOS_DEPLOYMENT_TARGET = ${IOS_DEPLOYMENT_TARGET}#g" \
      -e "s#Feature{{FEATURE_NAME}}#Feature${FEATURE_NAME}#g" \
      -e "s#feature{{FEATURE_NAME}}#feature${FEATURE_NAME}#g" \
      -e "s#feature/{{FEATURE_NAME}}#feature/${FEATURE_NAME_LOWER}#g" \
      -e "s#feature\\.{{FEATURE_NAME}}#feature.${FEATURE_NAME_LOWER}#g" \
      -e "s#feature:{{FEATURE_NAME}}#feature:${FEATURE_NAME_LOWER}#g" \
      -e "s#{{FEATURE_NAME}}ViewModel#${FEATURE_NAME}ViewModel#g" \
      -e "s#{{FEATURE_NAME}}Screen#${FEATURE_NAME}Screen#g" \
      -e "s#{{FEATURE_NAME}}Contract#${FEATURE_NAME}Contract#g" \
      -e "s#{{FEATURE_NAME}}StateUi#${FEATURE_NAME}StateUi#g" \
      -e "s#{{FEATURE_NAME}}IntentUi#${FEATURE_NAME}IntentUi#g" \
      -e "s#{{FEATURE_NAME}}EffectUi#${FEATURE_NAME}EffectUi#g" \
      -e "s#{{FEATURE_NAME}}ViewController#${FEATURE_NAME}ViewController#g" \
      -e "s#{{FEATURE_NAME}}#${FEATURE_NAME}#g" \
      -e "s#com/acme/outfit#${PACKAGE_PATH}#g" \
      -e "s#com\\.acme\\.outfit#${PACKAGE_BASE}#g" \
      -e "s#{{PACKAGE_PATH}}#${PACKAGE_PATH}#g" \
      -e "s#{{PACKAGE_BASE}}#${PACKAGE_BASE}#g" \
      -e "s#{{PROJECT_NAME}}#${PROJECT_NAME}#g" \
      -e "s#{{APP_ID}}#${APP_ID}#g" \
      -e "s#{{MIN_SDK}}#${MIN_SDK}#g" \
      -e "s#{{TARGET_SDK}}#${TARGET_SDK}#g" \
      -e "s#{{IOS_DEPLOYMENT_TARGET}}#${IOS_DEPLOYMENT_TARGET}#g" \
      -e "s#Acme#${PROJECT_NAME}#g"
}

# =============================================================================
# substitute_path: apply token substitutions to a path string
# Returns result on stdout.
# =============================================================================
substitute_path() {
    # Use python for path substitution — avoids all bash/zsh pattern escaping issues with
    # slashes and curly braces in patterns.
    python3 - "$1" <<PYEOF
import sys
p = sys.argv[1]
FEATURE_NAME = "${FEATURE_NAME}"
FEATURE_NAME_LOWER = "${FEATURE_NAME_LOWER}"
PACKAGE_PATH = "${PACKAGE_PATH}"
PACKAGE_BASE = "${PACKAGE_BASE}"
PROJECT_NAME = "${PROJECT_NAME}"
APP_ID = "${APP_ID}"
MIN_SDK = "${MIN_SDK}"
TARGET_SDK = "${TARGET_SDK}"
IOS_DEPLOYMENT_TARGET = "${IOS_DEPLOYMENT_TARGET}"
# Order: compound forms first, then generic token
p = p.replace("Feature{{FEATURE_NAME}}", "Feature" + FEATURE_NAME)
p = p.replace("feature/{{FEATURE_NAME}}", "feature/" + FEATURE_NAME_LOWER)
p = p.replace("feature.{{FEATURE_NAME}}", "feature." + FEATURE_NAME_LOWER)
p = p.replace("feature:{{FEATURE_NAME}}", "feature:" + FEATURE_NAME_LOWER)
p = p.replace("{{FEATURE_NAME}}", FEATURE_NAME)
p = p.replace("{{PACKAGE_PATH}}", PACKAGE_PATH)
p = p.replace("{{PACKAGE_BASE}}", PACKAGE_BASE)
p = p.replace("{{PROJECT_NAME}}", PROJECT_NAME)
p = p.replace("{{APP_ID}}", APP_ID)
p = p.replace("{{MIN_SDK}}", MIN_SDK)
p = p.replace("{{TARGET_SDK}}", TARGET_SDK)
p = p.replace("{{IOS_DEPLOYMENT_TARGET}}", IOS_DEPLOYMENT_TARGET)
print(p, end="")
PYEOF
}

# =============================================================================
# copy_template_tree: copies all *.template files from src_dir into dest_dir,
# stripping .template suffix and substituting tokens in content AND path.
# =============================================================================
copy_template_tree() {
    local src_dir="${1%/}"    # strip trailing slash if any
    local dest_dir="$2"

    while IFS= read -r -d '' tmpl; do
        local rel="${tmpl#${src_dir}/}"
        # Strip .template from filename
        local rel_stripped="${rel%.template}"
        # Substitute tokens in the path
        local rel_final
        rel_final="$(substitute_path "$rel_stripped")"

        local dest_file="$dest_dir/$rel_final"
        mkdir -p "$(dirname "$dest_file")"
        substitute_content < "$tmpl" > "$dest_file"
    done < <(find "$src_dir" -name "*.template" -type f -print0)
}

# =============================================================================
# copy_binary: copy non-template files verbatim (e.g. gradle-wrapper.jar, png)
# =============================================================================
copy_binary() {
    local src_dir="${1%/}"    # strip trailing slash if any
    local dest_dir="$2"

    while IFS= read -r -d '' f; do
        local rel="${f#${src_dir}/}"
        local rel_final
        rel_final="$(substitute_path "$rel")"
        local dest_file="$dest_dir/$rel_final"
        mkdir -p "$(dirname "$dest_file")"
        cp "$f" "$dest_file"
    done < <(find "$src_dir" -not -name "*.template" -type f -print0)
}

# =============================================================================
# Main copy operations
# =============================================================================

echo "--- Copying _root ---"
copy_template_tree "$TEMPLATES_DIR/_root" "$DEST"
copy_binary "$TEMPLATES_DIR/_root" "$DEST"

echo "--- Copying _build-logic ---"
copy_template_tree "$TEMPLATES_DIR/_build-logic" "$DEST/build-logic"
copy_binary "$TEMPLATES_DIR/_build-logic" "$DEST/build-logic"

echo "--- Copying _android-app ---"
copy_template_tree "$TEMPLATES_DIR/_android-app" "$DEST"
copy_binary "$TEMPLATES_DIR/_android-app" "$DEST"

echo "--- Copying _ios-app ---"
copy_template_tree "$TEMPLATES_DIR/_ios-app" "$DEST"
copy_binary "$TEMPLATES_DIR/_ios-app" "$DEST"

echo "--- Copying core-required ---"
for mod_dir in "$TEMPLATES_DIR/core-required"/*/; do
    mod_name="$(basename "$mod_dir")"
    echo "  core-required/$mod_name"
    copy_template_tree "$mod_dir" "$DEST/core/$mod_name"
    copy_binary "$mod_dir" "$DEST/core/$mod_name"
done

echo "--- Copying feature-main ---"
copy_template_tree "$TEMPLATES_DIR/feature-main" "$DEST/feature/$FEATURE_NAME_LOWER"
copy_binary "$TEMPLATES_DIR/feature-main" "$DEST/feature/$FEATURE_NAME_LOWER"

echo "--- Copying optional modules: ${(k)OPT_CLOSURE} ---"
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
# Patch settings.gradle.kts — append optional module includes
# =============================================================================
SETTINGS="$DEST/settings.gradle.kts"
if [[ -f "$SETTINGS" && ${#OPT_CLOSURE} -gt 0 ]]; then
    echo "" >> "$SETTINGS"
    echo "// Optional modules (closure: ${(k)OPT_CLOSURE})" >> "$SETTINGS"
    for mod in "${(k)OPT_CLOSURE[@]}"; do
        echo "include(\":core:${mod}\")" >> "$SETTINGS"
    done
fi

# =============================================================================
# Make gradlew executable
# =============================================================================
[[ -f "$DEST/gradlew" ]] && chmod +x "$DEST/gradlew"

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

echo ""
echo "Optional closure: ${(k)OPT_CLOSURE}"
echo "DONE."
