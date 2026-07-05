#!/bin/sh
set -e

MEILI_HOST="${MEILI_HOST:-http://localhost:7700}"
MEILI_KEY="${MEILI_MASTER_KEY:-masterKey}"
DATASET_URL="https://raw.githubusercontent.com/meilisearch/datasets/main/datasets/movies/movies.json"

echo "Waiting for Meilisearch at ${MEILI_HOST}..."
until curl -sf "${MEILI_HOST}/health" >/dev/null; do
  sleep 2
done
echo "Meilisearch is ready."

echo "Configuring movies index settings..."
curl -sf -X PATCH "${MEILI_HOST}/indexes/movies/settings" \
  -H "Authorization: Bearer ${MEILI_KEY}" \
  -H "Content-Type: application/json" \
  -d '{"searchableAttributes":["title","overview"],"filterableAttributes":["genres","release_date"],"sortableAttributes":["release_date"]}'

echo "Downloading and indexing dataset from ${DATASET_URL}..."
TMPFILE=$(mktemp)
curl -sL "${DATASET_URL}" -o "${TMPFILE}"
curl -sf -X POST "${MEILI_HOST}/indexes/movies/documents?primaryKey=id" \
  -H "Authorization: Bearer ${MEILI_KEY}" \
  -H "Content-Type: application/json" \
  --data-binary "@${TMPFILE}"
rm -f "${TMPFILE}"

echo "Seeding complete."
