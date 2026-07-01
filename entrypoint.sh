#!/bin/sh
set -eu

meilisearch --http-addr 127.0.0.1:7700 --db-path /meili_data --no-analytics &

for _ in $(seq 1 60); do
    if wget -q -O - http://127.0.0.1:7700/health >/dev/null 2>&1; then
        exec java -jar /app/app.jar
    fi
    sleep 1
done

echo "Meilisearch failed to become healthy" >&2
exit 1
