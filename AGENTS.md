# Engineering Principles

This document records the engineering principles established during the initial development of this project.
It exists to ensure that future agentic and human contributors follow the same practices.

## Dependencies

- **Keep all dependencies up to date.** Dependabot is configured for GitHub Actions, Gradle, and Docker ecosystems with a 3-day cooldown on all update types. Do not disable or bypass Dependabot.
- **Use a single source of truth for versions.** In the `Dockerfile`, all base image versions are declared as `ARG` at the top and referenced via those args in every `FROM` statement. Never hard-code a version string in more than one place.
- **Pin every external version explicitly.** Floating tags (e.g. `latest`) are not acceptable — they produce non-reproducible builds. This applies to Docker base images, GitHub Actions, Gradle dependencies, and any external resources fetched at build time.

## Docker / Container

- **Multi-stage builds.** Use separate stages for build-time work and the final runtime image to keep the runtime image small and free of build tooling.
- **Run as a non-root user.** The final runtime stage must create a dedicated system user (e.g. `app`) and switch to it with `USER`. Do not run services as root.
- **Copy binaries across stages rather than re-installing.** Where the same binary is used in a builder stage and the runtime stage (e.g. Meilisearch), copy it from the builder stage (`COPY --from=...`) rather than installing it separately. This guarantees the runtime binary is exactly the same version as the one used to produce build-time artefacts.
- **Bind internal services to localhost only.** Services that are not meant to be externally accessible (e.g. Meilisearch on port 7700) must be bound to `127.0.0.1`, never `0.0.0.0`.

## Process Management & Shutdown

- **Keep the shell as PID 1 in entrypoint scripts.** Do not `exec` into a child process when multiple processes need to be managed. Keep the shell as PID 1 so it can receive signals and relay them to all child processes.
- **Trap signals and clean up.** Entrypoint scripts must register a `trap` for `INT`, `TERM`, and `EXIT` that stops all background processes gracefully.
- **Poll health before starting dependents.** When one process depends on another being ready (e.g. the JVM app waiting for Meilisearch), poll the health endpoint in a bounded loop with a timeout rather than sleeping for a fixed duration.

## CI / GitHub Actions

- **Separate concerns into separate workflows.** Build/test and security scanning (CodeQL) live in separate workflow files because they require different permissions and serve different purposes. Do not merge them.
- **Grant minimal permissions.** Declare `permissions` explicitly at the workflow level. Use `contents: read` as the baseline; grant additional permissions only where required (e.g. `security-events: write` for CodeQL).
- **Avoid duplicate workflow runs on PRs.** Restrict the `push` trigger to `branches: [main]` so that pushes to PR branches only fire the `pull_request` event (not both). This prevents every commit on a PR branch from running each workflow twice.
- **Run CodeQL SAST on a weekly schedule** in addition to push/PR triggers so that newly discovered vulnerabilities in unchanged code are also caught.

## Security

- **Run SAST on every PR and push.** CodeQL analysis for the project's language(s) must run as part of CI on every pull request and push to `main`, as well as on a weekly schedule.
- **Set HTTP security headers on all responses.** Use the framework's headers plugin (Ktor `DefaultHeaders`) to add at minimum:
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Content-Security-Policy` — as strict as possible; avoid `'unsafe-inline'`
- **Avoid `'unsafe-inline'` in Content-Security-Policy.** Extract all inline `<style>` and `<script>` blocks to separate static files so the CSP can be restricted to `'self'` only.
- **HTML-escape all untrusted data before inserting into the DOM.** Never interpolate API response values directly into `innerHTML`. Always pass values through an escaping function first.

## Build & Test

- **Builds are driven through `make`.** Use `make build` (which runs `gradle --no-daemon test shadowJar`) as the canonical build command. CI workflows invoke `make build`, not Gradle directly.
- **Tests must pass before the fat JAR is produced.** The `build` Makefile target depends on `test`; this order must be preserved.
- **Use `--no-daemon` for Gradle in CI and Docker.** The Gradle daemon is not appropriate in non-interactive or containerised environments.
