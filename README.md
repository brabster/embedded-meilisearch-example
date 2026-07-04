# embedded-meilisearch-example

Example of embedding search in the application tier using Meilisearch as a pre-indexed sidecar alongside a Kotlin/Ktor pass-through API.

## Design

### Pre-indexed sidecar

A common trap when embedding search is runtime data management: the application polls a storage bucket for new data files and indexes them on the fly. This creates resource contention (indexing saturates compute, hanging the UI), brittle parsing (a single new JSON field breaks the parser and takes down the app), and complex rollback logic just to survive loading failures.

This example flips that model. The heavy lifting moves to build time. The CI pipeline pre-indexes data directly into Meilisearch and ships the result as a sidecar container alongside the application. The app and its data become a single atomic deployable unit. No polling, no UI hangs, no brittle parsing.

In this repo the seed data is bundled into the application jar and loaded on startup (controlled by `SEED_ON_STARTUP`). In a production pipeline you would build and publish a pre-seeded Meilisearch image instead, removing the startup seeding step entirely.

### Pass-through API

A second trap is building a strict class hierarchy to represent the search system's JSON output. Every time the frontend needs a new field or widget it requires a backend engineer to update Kotlin classes before any frontend work can ship.

This backend treats documents as `Map<String, Any?>` throughout. It forwards search parameters to Meilisearch and returns the response body verbatim to the client. The backend engineer is removed from the critical path for data-shape changes. The contract is strictly between the pipeline (data producer) and the frontend (data consumer).

## Endpoints

All endpoints are relative to `http://localhost:8080`.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/search/{index}` | Full-text search with optional filtering, sorting, and faceting |
| `GET` | `/api/fetch/{index}/{id}` | Retrieve a single document by primary key |
| `GET` | `/api/suggest/{index}` | Lightweight typeahead (returns a list of matching documents) |

### Search parameters (`/api/search/{index}`)

All parameters are optional.

| Parameter | Default | Description |
|-----------|---------|-------------|
| `q` | `""` | Search query |
| `filter` | — | Meilisearch filter expression |
| `sort` | — | Comma-separated sort fields, e.g. `rating:desc,title:asc` |
| `page` | `1` | Page number |
| `hitsPerPage` | `20` | Results per page |
| `facets` | — | Comma-separated attribute names for facet counts |

### Suggest parameters (`/api/suggest/{index}`)

| Parameter | Default | Description |
|-----------|---------|-------------|
| `q` | `""` | Partial query string |
| `limit` | `5` | Maximum number of suggestions |

## Configuration

| Environment variable | Default | Description |
|----------------------|---------|-------------|
| `MEILI_HOST` | `http://meilisearch:7700` | Meilisearch base URL |
| `MEILI_MASTER_KEY` | `masterKey` | Meilisearch API key |
| `SERVER_PORT` | `8080` | HTTP port the backend listens on |
| `SEED_ON_STARTUP` | `true` | Load seed data into Meilisearch on startup |

## Running locally

Requires Docker and Docker Compose.

```sh
make run
```

This builds the application image and starts both the Meilisearch sidecar and the backend. Seed data (movies and books) is loaded automatically on first start.

### Available indexes

| Index | Primary key |
|-------|-------------|
| `movies` | `id` |
| `books` | `id` |

Example search:

```sh
curl "http://localhost:8080/api/search/movies?q=batman&hitsPerPage=5"
```

## Building

Requires JDK 21.

```sh
make build
```

This runs tests and produces a fat jar at `build/libs/*-all.jar`.

## Tests

Unit tests use Ktor's `MockEngine` and do not require a running Meilisearch instance.

```sh
make test
```
