#!/bin/sh
# Run a fresh real-build matrix. Generated files and logs stay outside the repo.
set -eu
cli=$1
output=$2
group=$3
case "$cli" in /*) ;; *) echo 'Use an absolute launcher path' >&2; exit 1;; esac
case "$output" in /*) ;; *) echo 'Use an absolute output path' >&2; exit 1;; esac
if [ -e "$output" ]; then echo 'Matrix output must not exist' >&2; exit 1; fi
mkdir -p "$output"
status="$output/status.tsv"
: > "$status"
case "$group" in
  android) kind=android; ui=android-compose; targets='"android"'; navigations='none navigation3';;
  shared) kind=kmp; ui=shared-compose; targets='"android","ios","desktop","web"'; navigations='none navigation3';;
  native) kind=kmp; ui=swiftui; targets='"android","ios"'; navigations='none platform-native';;
  *) echo 'Group must be android, shared or native' >&2; exit 1;;
esac
failed=0
for di in manual koin; do
  for nav in $navigations; do
    for network in none ktor; do
      for storage in none settings; do
        id="$group-$di-$nav-$network-$storage"
        project="$output/$id"
        spec="$output/$id.json"
        cat > "$spec" <<JSON
{"name":"Aurora","package":"com.example.aurora","kind":"$kind","targets":[$targets],"ui":"$ui","di":"$di","navigation":"$nav","network":"$network","storage":"$storage","ios_deployment_target":"16.0"}
JSON
        echo "Starting $id"
        if ! "$cli" generate --config "$spec" --output "$project" > "$output/$id-generate.log" 2>&1; then
          printf '%s\tGENERATION_FAILED\n' "$id" >> "$status"; failed=1; continue
        fi
        if "$cli" verify "$project" --timeout 1200 > "$output/$id-verify.log" 2>&1; then
          printf '%s\tPASS\n' "$id" >> "$status"
          echo "PASS $id"
        else
          printf '%s\tFAIL\n' "$id" >> "$status"; failed=1
          echo "FAIL $id; see $project/.bootstrap"
        fi
      done
    done
  done
done
exit "$failed"
