# immutable-search-demo

Demo service that runs a Kotlin (Ktor) API and Meilisearch in the same container. The `movies` index is built at image build-time and copied into the final runtime image as immutable data.

## Build

Build and test the Kotlin app (works locally in a Codespace or in CI):

```bash
make build
```

Build the Docker image (requires Docker):

```bash
make docker-build
```

## Run

Only publish the API port. Do **not** publish Meilisearch port 7700.

```bash
make docker-run
```

Meilisearch is started inside the container and bound to `127.0.0.1:7700`, so it is not externally reachable.

## Query

```bash
curl "http://localhost:8080/api/search?q=batman"
```

## Makefile targets

| Target | Description |
|---|---|
| `make test` | Run unit tests |
| `make build` | Run tests and build fat JAR |
| `make docker-build` | Build the Docker image |
| `make docker-run` | Run the container on port 8080 |
