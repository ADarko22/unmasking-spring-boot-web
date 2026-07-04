# Unmasking Spring Boot Web

> Building the same REST API twice: once with Spring Boot, once with only the Java standard library.

Spring Boot is incredibly productive. With a few annotations like `@RestController`, `@PostMapping` `@RequestBody` and
`@PathVariable` you get a production-ready HTTP API with almost no boilerplate.
The trade-off is that many important HTTP concepts become invisible.

This project rebuilds the same URL shortener twice:

* **Spring Boot** – using the framework most Java developers know
* **Plain Java** – using `com.sun.net.httpserver.HttpServer` and explicit request handling

The goal is to understand **what Spring Boot is actually doing for you**,
so the framework becomes easier to reason about.

| Spring Boot          | Plain Java                 |
|----------------------|----------------------------|
| `@RestController`    | `HttpHandler`              |
| `@PostMapping`       | manual routing             |
| `@RequestBody`       | JSON parsing               |
| `ResponseEntity`     | status codes + headers     |
| dependency injection | manual object construction |

---

## What Spring Boot abstracts away

Both implementations expose exactly the same HTTP API.

The Spring Boot version delegates many responsibilities to the framework:

* request routing
* path matching
* request body parsing
* JSON serialization/deserialization
* dependency injection
* HTTP response construction
* error handling
* lifecycle management

The Plain Java implementation performs each of these steps explicitly.

Seeing these responsibilities in code helps clarify where framework behavior comes from and what actually happens during
request processing.

## Key observations

Building the same application twice highlights a few trade-offs.

### Spring Boot reduces accidental complexity

Most web applications spend very little effort on routing or JSON parsing.
Spring Boot removes this boilerplate so developers can focus on business logic.

### Explicit code reveals the HTTP protocol

The Plain Java implementation makes concepts that are usually implicit visible:

* matching HTTP methods
* matching URL paths
* reading the request body
* parsing JSON
* setting headers
* writing the response body
* choosing status codes

### Dependency Injection is just object creation

Spring Boot's IoC container creates and wires objects automatically.
The Plain Java version performs the same work manually, making constructor injection easier to understand.

### Annotations are configuration

Annotations such as `@RestController`, `@PostMapping`, and `@RequestBody` are metadata.

Spring scans these annotations during startup and builds the routing and request handling infrastructure automatically.

The Plain Java implementation performs this configuration explicitly.

---

## Running the applications

Requirements

- Java 21+
- Maven

Build everything

```bash
mvn clean package
```

Run Spring Boot (available at http://localhost:3000)

```bash
java -jar url-shortener-spring/target/url-shortener-spring.jar
```

Run the Plain Java implementation (available at http://localhost:3001)

```bash
java -jar url-shortener-native/target/url-shortener-native.jar
```

## Test the Short URL Service

**Spring**

```bash
curl -i \
  -X POST http://localhost:3000/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.example.com"}'
```

Expected response

```
HTTP/1.1 201 Created
Location: http://localhost:3000/abc123
```

```json
{
  "url": "https://www.example.com",
  "shortUrl": "http://localhost:3000/abc123"
}
```

**Plain Java**

```bash
curl -i \
  -X POST http://localhost:3001/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.example.com"}'
```

Expected response

```
HTTP/1.1 201 Created
Location: http://localhost:3001/abc123
```

```json
{
  "url": "https://www.example.com",
  "shortUrl": "http://localhost:3001/abc123"
}
```

---

## Test the URL Redirection

**Spring**

```
curl -i http://localhost:3000/abc123
```

```
HTTP/1.1 302 Found
Location: https://www.example.com
```

**Plain Java**

```
curl -i http://localhost:3001/abc123
```

```
HTTP/1.1 302 Found
Location: https://www.example.com
```

---

## Summary

| Concern              | Spring Boot                 | Plain Java                     |
|----------------------|-----------------------------|--------------------------------|
| Routing              | `@PostMapping`              | Regex matching                 |
| Controller discovery | Component scanning          | Manual registration            |
| Dependency Injection | IoC container               | Constructors                   |
| JSON                 | Jackson auto-configured     | Jackson called explicitly      |
| Request parsing      | `@RequestBody`              | Read `InputStream`             |
| Responses            | `ResponseEntity`            | Status + headers + bytes       |
| Redirect             | `ResponseEntity.location()` | Set `Location` header manually |

### **Spring annotation → Plain Java equivalent**

| Spring Boot                | Plain Java implementation                                                |
|----------------------------|--------------------------------------------------------------------------|
| `@SpringBootApplication`   | Create `HttpServer`, register routes, start server                       |
| `@RestController`          | `URLShortenerController` implementing handler methods                    |
| `@PostMapping("/shorten")` | `Route("POST", "^/shorten$")`                                            |
| `@GetMapping("/{id}")`     | Regex route + manual path extraction                                     |
| `@RequestBody`             | Read `InputStream` + `ObjectMapper.readValue()`                          |
| `ResponseEntity.created()` | Set `Location` header + send `201`                                       |
| Constructor injection      | Explicit `new URLShortenerService()` (or manual wiring in `Application`) |


