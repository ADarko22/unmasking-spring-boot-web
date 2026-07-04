# Unmasking Spring Boot Web

This repository demonstrates what Spring Boot Web does under the hood
by implementing the same functional HTTP REST API twice: once using standard Spring Boot,
and once using native Java (`com.sun.net.httpserver.HttpServer`).

The target application is a lightweight [URL Shortener](https://en.wikipedia.org/wiki/URL_shortening)
equipped with a static frontend UI.

---

## Architectural Comparison

| Architectural Concern    | Spring Boot Web (`:url-shortener-spring`)        | Plain Java (`:url-shortener-native`)                    |
|:-------------------------|:-------------------------------------------------|:--------------------------------------------------------|
| **Server Engine**        | Embedded Tomcat / Jetty                          | Native JDK `HttpServer` (utilizing Virtual Threads)     |
| **Request Routing**      | `@PostMapping`, `@GetMapping`                    | Custom Regex-matching `RequestDispatcher`               |
| **JSON Marshalling**     | Automatic Jackson integration via `@RequestBody` | Manual `ObjectMapper` payload streaming                 |
| **Response Lifecycle**   | Fluent `ResponseEntity` abstraction              | Manual headers, content-length, & socket output streams |
| **Static File Delivery** | Auto-served from `src/main/resources/static`     | Manual Classpath resource stream parsing                |
| **Dependency Wiring**    | IoC Container inversion of control               | Explicit object instantiation and injection             |

---

## Getting Started

### Prerequisites

* Java 21+
* Maven 3.9+

### Build the Monorepo

```bash
mvn clean package
```

### Running the Services

**The Spring Boot variant (UI at http://localhost:3000)**

```bash
java -jar url-shortener-spring/target/url-shortener-spring.jar
```

**The Plain Java variant (UI at http://localhost:3001)**

```bash
java -jar url-shortener-plain/target/url-shortener-plain.jar
```

---

## Verifying the Contract (Smoke Testing)

*Adjust the port based on the desired target service: 
3000 (Spring Boot variant) or 3001 (Native Java variant).*

### 1. Shorten a URL

```bash
curl -i -X POST http://localhost:3000/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://docs.oracle.com/en/java/"}'
```

### 2. Follow the Redirect

```bash
# Replace <hash> with the 5-character string returned from the step above
curl -i http://localhost:3000/<hash>
```