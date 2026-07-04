#!/bin/sh
set -eu

meilisearch --http-addr 127.0.0.1:7700 --db-path /meili_data --no-analytics &
meili_pid=$!

app_pid=""
cleanup() {
    [ -n "$app_pid" ] && kill "$app_pid" 2>/dev/null || true
    kill "$meili_pid" 2>/dev/null || true
}
trap cleanup INT TERM EXIT

for _ in $(seq 1 60); do
    if curl --fail --silent --show-error http://127.0.0.1:7700/health >/dev/null 2>&1; then
        java -jar /app/app.jar &
        app_pid=$!
        wait "$app_pid"
        exit $?
    fi
    sleep 1
done

echo "Meilisearch failed to become healthy" >&2
exit 1
