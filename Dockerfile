FROM getmeili/meilisearch:v1.48.3 AS meili-builder

RUN set -eux; \
    if command -v apk >/dev/null 2>&1; then \
      apk add --no-cache ca-certificates curl jq; \
    elif command -v apt-get >/dev/null 2>&1; then \
      apt-get update; \
      apt-get install -y --no-install-recommends curl jq ca-certificates; \
      rm -rf /var/lib/apt/lists/*; \
    fi; \
    mkdir -p /meili_data; \
    curl -fsSL -o /tmp/movies.json https://raw.githubusercontent.com/meilisearch/meilisearch/latest/datasets/movies.json; \
    meilisearch --http-addr 127.0.0.1:7700 --db-path /meili_data --no-analytics >/tmp/meili.log 2>&1 & \
    pid=$!; \
    for _ in $(seq 1 60); do \
      if curl -fsS http://127.0.0.1:7700/health >/dev/null; then break; fi; \
      sleep 1; \
    done; \
    task_uid=$(curl -fsS -X POST 'http://127.0.0.1:7700/indexes/movies/documents' \
      -H 'Content-Type: application/json' \
      --data-binary @/tmp/movies.json | jq -r '.taskUid'); \
    if [ -z "$task_uid" ] || [ "$task_uid" = "null" ]; then \
      echo 'Unable to parse task uid'; \
      cat /tmp/meili.log; \
      kill "$pid"; \
      exit 1; \
    fi; \
    for _ in $(seq 1 120); do \
      status=$(curl -fsS "http://127.0.0.1:7700/tasks/$task_uid" | jq -r '.status'); \
      if [ "$status" = "succeeded" ]; then break; fi; \
      if [ "$status" = "failed" ]; then \
        echo 'Indexing failed'; \
        kill "$pid"; \
        exit 1; \
      fi; \
      sleep 1; \
    done; \
    [ "$(curl -fsS "http://127.0.0.1:7700/tasks/$task_uid" | jq -r '.status')" = "succeeded" ]; \
    kill "$pid"; \
    wait "$pid" || true

FROM gradle:9.6.1-jdk17 AS kotlin-builder
WORKDIR /workspace
RUN set -eux; \
    if command -v apk >/dev/null 2>&1; then \
      apk add --no-cache ca-certificates; \
    elif command -v apt-get >/dev/null 2>&1; then \
      apt-get update; \
      apt-get install -y --no-install-recommends ca-certificates; \
      rm -rf /var/lib/apt/lists/*; \
    fi
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
RUN gradle --no-daemon shadowJar

FROM alpine:3.22
RUN apk add --no-cache openjdk17-jre getmeili-bin curl \
    && addgroup -S app \
    && adduser -S app -G app
WORKDIR /app
COPY --from=meili-builder /meili_data /meili_data
COPY --from=kotlin-builder /workspace/build/libs/*-all.jar /app/app.jar
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh && chown -R app:app /meili_data /app /entrypoint.sh
USER app
EXPOSE 8080
ENV HOST=0.0.0.0
ENV PORT=8080

ENTRYPOINT ["/entrypoint.sh"]
