#!/bin/sh
set -e

MEILI_HOST="${MEILI_HOST:-http://localhost:7700}"
MEILI_KEY="${MEILI_MASTER_KEY:-masterKey}"
DATASET_URL="https://raw.githubusercontent.com/meilisearch/datasets/main/datasets/movies/movies.json"

wait_for_task() {
  TASK_UID=$(echo "$1" | grep -o '"taskUid":[0-9]*' | grep -o '[0-9]*')
  echo "Waiting for task ${TASK_UID}..."
  while true; do
    STATUS=$(curl -sf -H "Authorization: Bearer ${MEILI_KEY}" "${MEILI_HOST}/tasks/${TASK_UID}" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    [ "${STATUS}" = "succeeded" ] && return 0
    [ "${STATUS}" = "failed" ] && echo "Task ${TASK_UID} failed." && return 1
    sleep 2
  done
}

echo "Waiting for Meilisearch at ${MEILI_HOST}..."
until curl -sf "${MEILI_HOST}/health" >/dev/null; do
  sleep 2
done
echo "Meilisearch is ready."

echo "Configuring movies index settings..."
SETTINGS_RESPONSE=$(curl -fS -X PATCH "${MEILI_HOST}/indexes/movies/settings" \
  -H "Authorization: Bearer ${MEILI_KEY}" \
  -H "Content-Type: application/json" \
  -d '{"searchableAttributes":["title","overview"],"filterableAttributes":["genres","release_date"],"sortableAttributes":["release_date"]}')
wait_for_task "${SETTINGS_RESPONSE}"

echo "Downloading dataset from ${DATASET_URL}..."
TMPFILE=$(mktemp)
curl -fL "${DATASET_URL}" -o "${TMPFILE}"

echo "Indexing documents..."
INGEST_RESPONSE=$(curl -fS -X POST "${MEILI_HOST}/indexes/movies/documents?primaryKey=id" \
  -H "Authorization: Bearer ${MEILI_KEY}" \
  -H "Content-Type: application/json" \
  --data-binary "@${TMPFILE}")
rm -f "${TMPFILE}"
wait_for_task "${INGEST_RESPONSE}"

echo "Seeding complete."
