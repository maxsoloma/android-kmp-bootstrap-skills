#!/bin/sh
# Exercise layouts that a four-platform project can accidentally hide.
set -eu
jar=$1
output=$2
case "$jar:$output" in /*:/*) ;; *) echo 'Use absolute JAR and output paths' >&2; exit 1;; esac
if [ -e "$output" ]; then echo 'Subset output must not exist' >&2; exit 1; fi
mkdir -p "$output"
failed=0
for id in desktop web ios-compose ios-swiftui android-kmp; do
  kind=kmp; ui=shared-compose; di=koin; navigation=navigation3; ios=15.0
  case "$id" in
    desktop) targets='"desktop"';;
    web) targets='"web"';;
    ios-compose) targets='"ios"';;
    ios-swiftui) targets='"ios"'; ui=swiftui; navigation=none;;
    android-kmp) targets='"android"';;
  esac
  cat > "$output/$id.json" <<JSON
{"name":"Subset","package":"dev.example.subset","kind":"$kind","targets":[$targets],"ui":"$ui","di":"$di","navigation":"$navigation","network":"ktor","storage":"settings","ios_deployment_target":"$ios","sdk":{"min":23,"target":35,"compile":37}}
JSON
  echo "Starting $id"
  if java -jar "$jar" generate --config "$output/$id.json" --output "$output/$id" > "$output/$id-generate.log" 2>&1 &&
     java -jar "$jar" verify "$output/$id" --timeout 1200 > "$output/$id-verify.log" 2>&1; then
    printf '%s\tPASS\n' "$id" >> "$output/status.tsv"
    echo "PASS $id"
  else
    printf '%s\tFAIL\n' "$id" >> "$output/status.tsv"
    echo "FAIL $id; see generation log and $output/$id/.bootstrap"
    failed=1
  fi
done
exit "$failed"
