# Custom Mock Server

A lightweight HTTP mock server built with Spring Boot. It intercepts any request (`/**`), looks up a matching rule, simulates a configurable delay, returns the configured response and exposes access metrics. Everything runs in memory, with no database required.

## Features

- **Wildcard routing**: a catch-all controller handles any HTTP method and path.
- **Rule matching**: Ant-style patterns (`/api/users/{id}`, `/api/**`, `*`). The most specific rule wins, and an exact HTTP method beats `*`.
- **Simulated latency**: per-rule `delayMs`.
- **Custom responses**: status code, headers and JSON body per rule.
- **Metrics**: total hits, per-rule hit count, average latency, last access time and unmatched requests.
- **Runtime management**: create, list and delete rules through an admin API, or reload the rules file without restarting.

## Tech Stack

- Java 17
- Spring Boot 4.1.x (Spring Web MVC)
- Maven

## Project Structure

```
src/main/java/com/example/mockserver/
├── CustomMockServerApplication.java   # Spring Boot entry point
├── config/
│   └── RulesLoader.java               # Loads rules from the JSON file at startup / on reload
├── controller/
│   ├── MockController.java            # Catch-all controller (/**)
│   └── AdminController.java           # Admin API (/__admin/**)
├── model/
│   ├── MockRule.java                  # Rule definition (record)
│   ├── RuleMetrics.java               # Per-rule metrics (record)
│   └── MetricsSnapshot.java           # Aggregated metrics (record)
└── service/
    ├── RuleService.java               # In-memory rule storage and matching
    └── MetricsService.java            # Thread-safe access metrics
src/main/resources/
├── application.properties
└── mocks/rules.json                   # Initial rules
```

## Getting Started

### Prerequisites

- JDK 17 or later
- Maven (or use the included Maven Wrapper)

### Run

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

You can also run `CustomMockServerApplication` directly from your IDE. The server starts on port **8080**.

### Configuration

`src/main/resources/application.properties`:

```properties
server.port=8080
mock.rules-file=classpath:mocks/rules.json
```

To use an external rules file (so you can edit it without rebuilding):

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--mock.rules-file=file:./rules.json
```

After editing the file, call `POST /__admin/reload`.

## Rule Format

Rules are defined as a JSON array:

```json
[
  {
    "id": "list-users",
    "method": "GET",
    "path": "/api/users",
    "status": 200,
    "delayMs": 500,
    "headers": { "X-Mock": "true" },
    "body": [
      { "id": 1, "name": "Ana" },
      { "id": 2, "name": "Bruno" }
    ]
  }
]
```

| Field     | Required | Default            | Description                                              |
|-----------|----------|--------------------|----------------------------------------------------------|
| `path`    | yes      | -                  | Ant-style pattern, e.g. `/api/users/{id}`, `/api/**`     |
| `id`      | no       | random UUID        | Unique rule identifier                                   |
| `method`  | no       | `*`                | HTTP method, or `*` for any method                       |
| `status`  | no       | `200`              | HTTP status code to return                               |
| `headers` | no       | `{}`               | Response headers                                         |
| `body`    | no       | `null`             | JSON object, array or string                             |
| `delayMs` | no       | `0`                | Artificial delay in milliseconds                         |

If a rule does not set `Content-Type`, the response uses `application/json`.

## API

### Mocked routes

Any request that does not match an admin endpoint is handled by the rules. If no rule matches, the server returns `404` with a JSON error and records the request as unmatched.

### Admin API (`/__admin`)

| Method   | Endpoint                | Description                         |
|----------|-------------------------|-------------------------------------|
| `GET`    | `/__admin/rules`        | List active rules                   |
| `POST`   | `/__admin/rules`        | Create or replace a rule            |
| `DELETE` | `/__admin/rules/{id}`   | Delete a rule                       |
| `DELETE` | `/__admin/rules`        | Delete all rules                    |
| `POST`   | `/__admin/reload`       | Reload rules from the rules file    |
| `GET`    | `/__admin/metrics`      | Get access metrics                  |
| `DELETE` | `/__admin/metrics`      | Reset metrics                       |

## Usage Examples

```bash
# Rule with a 500 ms delay
curl -i -w "\nTotal time: %{time_total}s\n" http://localhost:8080/api/users

# Path variable
curl -i http://localhost:8080/api/users/42

# Same path, different method
curl -i -X POST http://localhost:8080/api/users

# Simulated failure (500 after ~1.5 s)
curl -i http://localhost:8080/api/error/anything

# No matching rule (404)
curl -i http://localhost:8080/does/not/exist
```

Create a rule at runtime. First save this as `new-rule.json`:

```json
{
  "id": "ping",
  "method": "GET",
  "path": "/ping",
  "status": 200,
  "delayMs": 100,
  "headers": { "Content-Type": "text/plain" },
  "body": "pong"
}
```

Then:

```bash
curl -i -X POST http://localhost:8080/__admin/rules \
  -H "Content-Type: application/json" \
  -d @new-rule.json

curl -i http://localhost:8080/ping
```

Check the metrics:

```bash
curl http://localhost:8080/__admin/metrics
```

Example response:

```json
{
  "startedAt": "2026-10-11T22:30:00Z",
  "totalRequests": 6,
  "matchedRequests": 5,
  "unmatchedRequests": 1,
  "rules": [
    {
      "ruleId": "list-users",
      "method": "GET",
      "path": "/api/users",
      "hits": 2,
      "avgLatencyMs": 503.5,
      "lastAccess": "2026-10-11T22:31:10Z"
    }
  ],
  "unmatchedPaths": { "GET /does/not/exist": 1 }
}
```

## Notes

- Rules and metrics live in memory and are lost when the server stops. The rules file is reloaded on startup.
- Delays use `Thread.sleep`. On Java 21+, enable `spring.threads.virtual.enabled=true` to avoid tying up Tomcat threads.
- Browsers also request `/favicon.ico`, which will show up as an unmatched path in the metrics.

## Ideas for Improvement

- Placeholders in the response body (e.g. `{id}` taken from the path)
- Matching by header or query parameter
- Random failures (e.g. 10% of requests return 503)
- Persisting rules created via the API back to the file
- Automated tests with `MockMvc`

## License

This project is for study and testing purposes. Add a license of your choice if you plan to share it.
