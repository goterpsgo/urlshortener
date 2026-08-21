# TESTING

Your project uses Testcontainers, so here's how it works, tied to your actual setup in `UrlshortenerApplicationTests.java`.

## Core idea

Testcontainers is a Java library that spins up real Docker containers (databases, message brokers, etc.) for your tests, then tears them down automatically — so integration tests run against a real dependency instead of a mock or in-memory fake.

## How it works mechanically

1. Docker requirement: Testcontainers talks to your local Docker daemon via its API. Nothing runs unless Docker is available on the machine executing the tests.
2. `@Testcontainers` + `@Container`: The `@Testcontainers` class annotation hooks into JUnit 5's lifecycle. Fields annotated `@Container` are managed automatically — started before tests run, stopped after.
3. Container definition: new `PostgreSQLContainer<>("postgres:17-alpine")` pulls (if needed) and starts that exact image. Testcontainers assigns it a random host port to avoid collisions, and waits for a readiness check (e.g., Postgres accepting connections) before letting tests proceed.
4. Static field = container reuse: Because postgres is static, JUnit starts it once per test class and shares it across all `@Test` methods in that class, rather than restarting per test (faster, and matches typical singleton-datasource usage).
5. `@ServiceConnection`: This is Spring Boot's integration piece (`spring-boot-testcontainers` dependency). It auto-configures Spring's datasource properties (URL, username, password) to point at the running container — you don't need to manually wire `spring.datasource.url` etc. Spring detects the container type and configures the matching connection.
6. Teardown: After the test class finishes, Testcontainers stops and removes the container (via a JVM shutdown hook / Ryuk cleanup container), so no leftover containers pile up on the host.

## Why this matters here

Your `contextLoads()` test boots the full Spring context against a real Postgres instance rather than H2 or a mock — catching dialect-specific SQL issues, migration problems, or connection config bugs that an in-memory substitute would miss.

One note: since this spins up real containers, running these tests requires Docker to be running locally and in CI — worth confirming your CI pipeline has Docker-in-Docker or a Docker socket available if you haven't already.

# SETUP

To bring Testcontainers into another project, here's what's needed — using this repo's setup as the reference (Spring Boot 4.1.0 parent, Maven, Postgres container):

1. Dependencies (Maven)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-postgresql</artifactId> <!-- swap for mysql, kafka, etc. -->
    <scope>test</scope>
</dependency>
```
Note: this project doesn't pin a Testcontainers version explicitly — the spring-boot-starter-parent (4.1.0) BOM manages it (currently resolves to Testcontainers `2.0.5` — confirm with `mvn dependency:tree | grep testcontainers`). If the other project isn't a Spring Boot project (or uses an older Boot version without that BOM), you'll need to add the Testcontainers BOM yourself:
```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>testcontainers-bom</artifactId>
      <version>2.0.x</version> <!-- check current release -->
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

For Gradle, it's the equivalent `testImplementation` entries plus a `testImplementation platform("org.testcontainers:testcontainers-bom:...")`.

2. Test class wiring

Same pattern as `UrlshortenerApplicationTests.java`:
```java
import org.testcontainers.postgresql.PostgreSQLContainer; // note: relocated, non-generic package in Testcontainers 2.x

@SpringBootTest
@Testcontainers
class MyIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
}
```
`@ServiceConnection` only auto-wires Spring datasource properties if the project uses Spring Boot. For non-Spring projects, you'd manually pull `postgres.getJdbcUrl()/getUsername()/getPassword()` and set them on your own connection pool/config.

If you're on an older Testcontainers 1.x line instead, the class is still the generic `org.testcontainers.containers.PostgreSQLContainer<?>` — check your resolved version first (see the BOM note above) before copying either form.

3. Environment prerequisites (the part people usually miss)

- Docker daemon must be running on whatever machine executes the tests — local dev and CI both.
- CI runner needs Docker access. GitHub Actions' standard Linux runners have Docker preinstalled; other CI systems (e.g., some GitLab/Jenkins setups) may need Docker-in-Docker or a mounted socket explicitly enabled.
- Image pull access: if the CI runner is network-restricted, it needs to reach Docker Hub (or your internal registry mirror) to pull `postgres:17-alpine` or whatever image the new project needs.
- Ryuk (cleanup container): enabled by default, requires the daemon socket be reachable; if the CI environment blocks that, you'd set `TESTCONTAINERS_RYUK_DISABLED=true` (only as a last resort — it disables automatic container cleanup).

4. Nothing else is required for this project's pattern

There's no `docker-compose.yml` or custom `Dockerfile` in this repo driving Testcontainers — it's all Java-side container definitions, which is the simplest path to port. If the other project needs multiple coordinated containers (e.g., Postgres + Kafka + Redis), you'd add one `@Container` field per service, or reach for `ComposeContainer` if you'd rather define them in a compose file.

One thing worth checking: what dependency does the other project actually need (Postgres, MySQL, Kafka, Localstack, generic image)? That determines which `testcontainers-*` module to add.

# HOW TO RUN

## Prerequisite

Docker must be running locally before you run tests — `UrlshortenerApplicationTests` starts a real Postgres container via Testcontainers. If Docker isn't running, the test fails with a connection error to the Docker daemon (e.g. `Could not find a valid Docker environment`).

`DATASOURCE_URL` and friends don't need to be set — `@ServiceConnection` on the `postgres` field auto-configures the datasource against the running container, overriding whatever is in `application.yml`. However, `JWT_SECRET` (bound via `@Value("${app.jwt.secret}")` in `JwtService`, no default value) and, as of the admin-bootstrap migration, `ADMIN_USERNAME`/`ADMIN_PASSWORD_HASH` (used as Flyway placeholders in `V5__seed_admin_user.sql`, also no defaults) still must resolve for the Spring context to start, since `@SpringBootTest` boots the full application and runs all migrations — Testcontainers only replaces the datasource, not other required properties. Provide them via a local `.env` file (gitignored; see `.env.example`) or exported env vars before running tests.

## Run all tests

```bash
./mvnw test
```

First run pulls the `postgres:17-alpine` image if it isn't already cached locally, so expect it to be slower once and faster on subsequent runs.

## Run a single test class

```bash
./mvnw test -Dtest=UrlshortenerApplicationTests
```

## Run a single test method

```bash
./mvnw test -Dtest=UrlshortenerApplicationTests#contextLoads
```

## Skip tests (e.g. for a quick local build)

```bash
./mvnw install -DskipTests
```

## Troubleshooting

- **"Could not find a valid Docker environment" / connection refused**: start Docker Desktop (or your Docker daemon) and re-run.
- **Image pull failures**: check network access to Docker Hub, or that an internal registry mirror is configured if your network restricts external pulls.
- **Container fails to become ready / timeout**: usually a resource issue (low memory/CPU on the Docker host) — increase Docker Desktop's resource allocation.

# FRONTEND E2E (Playwright)

`frontend/` has a Playwright suite (`@playwright/test`) that drives the real app in a browser — register → shorten a URL → edit it from the Links page — rather than testing components in isolation.

## Setup (one-time)

```bash
cd frontend
npm install
npx playwright install chromium
```

`npx playwright install` downloads a pinned Chromium build to `~/Library/Caches/ms-playwright` (or the OS equivalent). If your network sits behind a proxy that intercepts `cdn.playwright.dev` with a certificate Node doesn't trust, this download will fail with `SELF_SIGNED_CERT_IN_CHAIN` — that's a proxy/network config issue, not a Playwright config issue; get the download unblocked (or add the proxy's CA to `NODE_EXTRA_CA_CERTS`) rather than working around it in the suite.

## Run the tests

```bash
cd frontend
npm run test:e2e        # headless
npm run test:e2e:ui     # Playwright's UI mode, for debugging
```

`playwright.config.js`'s `webServer` entries start the Vite dev server (port 5173) and the Spring Boot backend (`./mvnw spring-boot:run` from the repo root, port 8080) automatically if they aren't already running, and reuse them if they are (`reuseExistingServer: true`) — so `npm run test:e2e` works standalone or alongside `hivemind`. The backend needs the same `.env` (`JWT_SECRET`, etc.) as normal local dev.

Each test run registers a fresh, uniquely-named user (`e2e_<timestamp>_<random>`) against the H2 in-memory database, so runs don't collide with each other or leave state you need to clean up.

A failed run writes a trace/screenshot to `frontend/test-results/` and an HTML report to `frontend/playwright-report/` (both gitignored) — open the report with `npx playwright show-report` from `frontend/`.
