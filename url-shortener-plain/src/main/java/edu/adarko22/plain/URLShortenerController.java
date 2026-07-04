package edu.adarko22.plain;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;

public class URLShortenerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(URLShortenerController.class);
    private final URLShortenerService urlShortenerService = new URLShortenerService();
    private final ObjectMapper mapper = new ObjectMapper();

    record ShortenerRequest(String url) {
    }

    record ShortenerResponse(String url, String shortUrl) {
    }

    void urlShortener(HttpExchange exchange) {
        var request = parseShortenerRequest(exchange);
        var shortUrl = urlShortenerService.shortenUrl(request.url);

        if (shortUrl.isPresent()) {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            var shortUrlUri = URI.create(shortUrl.get());
            exchange.getResponseHeaders().add("Location", shortUrlUri.toString());

            var response = new ShortenerResponse(request.url, shortUrl.get());
            var responseBytes = mapper.writeValueAsBytes(response);

            sendResponse(exchange, 201, responseBytes);
        } else {
            sendResponse(exchange, 400, "Bad Request: malformed url".getBytes());
        }
    }

    void redirect(HttpExchange exchange) {
        // resolving the URI path after the initial "/"
        var hash = exchange.getRequestURI().getPath().substring(1);
        var originalUrl = urlShortenerService.restoreUrl(hash);

        if (originalUrl.isPresent()) {
            exchange.getResponseHeaders().add("Location", originalUrl.get().toString());
            sendResponse(exchange, 302, new byte[0]);
        } else {
            sendResponse(exchange, 404, "Not Found".getBytes());
        }
    }

    private ShortenerRequest parseShortenerRequest(HttpExchange exchange) {
        try (var inputStream = exchange.getRequestBody()) {
            return mapper.readValue(inputStream, ShortenerRequest.class);
        } catch (IOException e) {
            var errorMessage = "Error parsing HttpExchange as %s".formatted(ShortenerRequest.class.getSimpleName());
            LOGGER.error(errorMessage);
            throw new IllegalStateException(errorMessage, e);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, byte[] payload) {
        try (var os = exchange.getResponseBody()) {
            exchange.sendResponseHeaders(statusCode, payload.length);
            os.write(payload);
        } catch (IOException e) {
            var errorMessage = "Failed to send HTTP response for request %s %s"
                    .formatted(exchange.getRequestMethod(), exchange.getRequestURI());
            LOGGER.error(errorMessage);
            throw new IllegalStateException(errorMessage, e);
        }
    }
}
