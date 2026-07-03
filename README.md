# Unmasking Spring Boot Web

This repository contains two implementations of a simple URL Shortener Service:

- [Application in Spring Boot](url-shortener-spring/src/main/java/edu/adarko22/spring/Application.java)

  Implements the URL Shortener described below using Spring Boot Web framework.

- [Application in Native Java](url-shortener-native/src/main/java/edu/adarko22/nativ/Application.java)

  Implements the URL Shortener described below using native Java and a JSON parsing library.

The intent is to understand what happens under the hood when using Spring Boot Annotations,
such as `@RestController`,`@PostMapping`, `@RequestBody`, `@GetMapping` and `PathVariable`.

This helps appreciate the big help from the framework, especially with:
- Request Routing
- Data Serialization/Deserialization
- Thread Management

## URL Shortener Case Study

### Shorten Endpoint

Provide a URL to be shortened, and receive a shortened version in response.

- **Request:**
    ```sh
    curl -X POST http://localhost:3000/shorten \
    -H "Content-Type: application/json" \
    -d '{"url": "https://www.example.com"}'
    ```
- **Response:**
    - **Status Code:** 201 Created
      ```
      Location: http://localhost:3000/abc123
      ```
      ```json
      {
      "url": "https://www.example.com",
      "shortened_url": "http://localhost:3000/abc123"
      }
      ``` 
    - **Status Code:** 400 Bad Request

      For invalid requests (e.g., missing or malformed `url`, invalid JSON)

### Redirect Endpoint

Access the shortened URL to be redirected to the original URL.

- **Request:**
    ```sh
    curl -X GET http://localhost:3000/abc123
    ```
- **Response:**
    - **Status Code:** 302 Found
      ```
      Location: https://www.example.com
      ```
      ```json
      {
      "url": "https://www.example.com",
      "shortened_url": "http://localhost:3000/abc123"
      }
      ``` 
    - **Status Code:** 404 Not Found

      If the shortened code does not exist