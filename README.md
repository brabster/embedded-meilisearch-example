# immutable-search-demo

Demo service that runs a Kotlin (Ktor) API and Meilisearch in the same container. The `movies` index is built at image build-time and copied into the final runtime image as immutable data.

## Build

```bash
docker build -t immutable-search-demo .
```

## Run

Only publish the API port. Do **not** publish Meilisearch port 7700.

```bash
docker run --rm -p 8080:8080 immutable-search-demo
```

Meilisearch is started inside the container and bound to `127.0.0.1:7700`, so it is not externally reachable.

## Query

```bash
curl "http://localhost:8080/api/search?q=batman"
```
