#!/bin/sh
set -eux

if command -v apk >/dev/null 2>&1; then
    apk add --no-cache ca-certificates curl jq
elif command -v apt-get >/dev/null 2>&1; then
    apt-get update
    apt-get install -y --no-install-recommends curl jq ca-certificates
    rm -rf /var/lib/apt/lists/*
fi

mkdir -p /meili_data
curl -fsSL -o /tmp/movies.json https://raw.githubusercontent.com/meilisearch/meilisearch/latest/datasets/movies.json

meilisearch --http-addr 127.0.0.1:7700 --db-path /meili_data --no-analytics >/tmp/meili.log 2>&1 &
pid=$!

for _ in $(seq 1 60); do
    if curl -fsS http://127.0.0.1:7700/health >/dev/null; then break; fi
    sleep 1
done

task_uid=$(curl -fsS -X POST 'http://127.0.0.1:7700/indexes/movies/documents' \
    -H 'Content-Type: application/json' \
    --data-binary @/tmp/movies.json | jq -r '.taskUid')

if [ -z "$task_uid" ] || [ "$task_uid" = "null" ]; then
    echo 'Unable to parse task uid'
    cat /tmp/meili.log
    kill "$pid"
    exit 1
fi

for _ in $(seq 1 120); do
    status=$(curl -fsS "http://127.0.0.1:7700/tasks/$task_uid" | jq -r '.status')
    if [ "$status" = "succeeded" ]; then break; fi
    if [ "$status" = "failed" ]; then
        echo 'Indexing failed'
        kill "$pid"
        exit 1
    fi
    sleep 1
done

[ "$(curl -fsS "http://127.0.0.1:7700/tasks/$task_uid" | jq -r '.status')" = "succeeded" ]

kill "$pid"
wait "$pid" || true
