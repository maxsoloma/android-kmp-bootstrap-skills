#!/bin/sh
# SDK/build verification only: no cloud projects, credentials or live app startup.
set -eu
cli=$1
output=$2
case "$cli" in /*) ;; *) echo 'Use an absolute launcher path' >&2; exit 1;; esac
case "$output" in /*) ;; *) echo 'Use an absolute output path' >&2; exit 1;; esac
if [ -e "$output" ]; then echo 'Verification output must not exist' >&2; exit 1; fi
mkdir -p "$output"
status="$output/status.tsv"
: > "$status"
failed=0
for recipe in android shared native; do
  case "$recipe" in
    android) kind=android; ui=android-compose; nav=navigation3; targets='"android"';;
    shared) kind=kmp; ui=shared-compose; nav=navigation3; targets='"android","ios","desktop","web"';;
    native) kind=kmp; ui=swiftui; nav=platform-native; targets='"android","ios"';;
  esac
  config="$output/$recipe.json"
  cat > "$config" <<JSON
{"name":"Aurora","package":"com.example.aurora","kind":"$kind","ui":"$ui","navigation":"$nav","targets":[$targets],"di":"koin","network":"ktor","storage":"settings","ios_deployment_target":"16.0","firebase":{"services":["analytics","messaging","remote-config"]}}
JSON
  if ! "$cli" generate --config "$config" --output "$output/$recipe" > "$output/$recipe-generate.log" 2>&1; then
    printf '%s\tGENERATION_FAILED\n' "$recipe" >> "$status"; failed=1; continue
  fi
  if "$cli" verify "$output/$recipe" --timeout 1200 > "$output/$recipe-verify.log" 2>&1; then
    printf '%s\tPASS\n' "$recipe" >> "$status"
  else
    printf '%s\tFAIL\n' "$recipe" >> "$status"; failed=1
  fi
done
exit "$failed"
