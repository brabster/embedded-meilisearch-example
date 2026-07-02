ARG MEILISEARCH_VERSION=v1.48.3
ARG GRADLE_VERSION=9.6.1-jdk21
ARG DEBIAN_VERSION=12-slim

FROM getmeili/meilisearch:${MEILISEARCH_VERSION} AS meili-builder

COPY build-index.sh /build-index.sh
RUN chmod +x /build-index.sh && /build-index.sh

FROM gradle:${GRADLE_VERSION} AS kotlin-builder
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

FROM debian:${DEBIAN_VERSION}
RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-21-jre-headless curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r app \
    && useradd -r -g app app
WORKDIR /app
COPY --from=meili-builder /bin/meilisearch /usr/local/bin/meilisearch
COPY --from=meili-builder /meili_data /meili_data
COPY --from=kotlin-builder /workspace/build/libs/*-all.jar /app/app.jar
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh && chown -R app:app /meili_data /app /entrypoint.sh
USER app
EXPOSE 8080
ENV HOST=0.0.0.0
ENV PORT=8080

ENTRYPOINT ["/entrypoint.sh"]
