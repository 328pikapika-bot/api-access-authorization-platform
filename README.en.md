# API Access and Authorization Platform

English | [简体中文](README.md)

An API publishing platform for providers, consumers, and administrators. It manages an API catalog, access requests and approvals, credentials, sandbox integration, and audit records.

## Capabilities

- The portal lets consumers browse APIs and request access; the admin UI manages resources and authorization scopes.
- The Java backend handles user admission, roles, category permissions, and API credentials.
- A separate sandbox service proxies integration requests and records calls for auditing.

## Code and setup

`源码/open-platform-ui/` is the portal, `源码/open-platform-admin/` the admin UI, `源码/open-platform-server/` the Spring Boot backend, and `源码/sandbox-service/` the sandbox. You need JDK 11, Maven, Node.js, and your own MariaDB/MySQL, Redis, MinIO, and identity services. Configure each module before starting it. Backend configuration is under `源码/open-platform-server/src/main/resources/`.

Run the backend unit tests from `源码/` with `mvn -f open-platform-server/pom.xml test`. This does not start the services.

## Configuration

Set `OPEN_PLATFORM_JWT_SECRET` to a random value of at least 32 bytes. Provide `TEST_DB_PASSWORD`, `TEST_REDIS_PASSWORD`, and `TEST_MINIO_SECRET_KEY` for the respective services. Certificate helper scripts read `NSTL_PFX_PASSWORD`. See the `.env.example` files under `源码/`; supply real values through local environment variables or ignored `.env` files. `127.0.0.1` and `example.invalid` are examples, not production endpoints.

## Verification and limits

The sandbox's allowed target URLs, authorization boundaries, and identity integration need verification in your environment. Review `start.bat` and HTTPS helper scripts before running them because they may change hosts/certificates or reclaim ports. The public code was statically reviewed on 2026-11-04; publishing it does not establish deployment readiness or full functional coverage.

The [publication notes](仓库发布说明.md) describe the included files. Local knowledge-base archives remain outside the repository.
