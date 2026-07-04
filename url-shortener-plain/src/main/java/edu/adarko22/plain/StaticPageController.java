package edu.adarko22.plain;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StaticPageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticPageController.class);

    void index(HttpExchange exchange) {
        try (var input = getClass().getResourceAsStream("/index.html")) {
            if (input == null) {
                sendResponse(exchange, 404, "index.html not found".getBytes());
                return;
            }

            var bytes = input.readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            sendResponse(exchange, 200, bytes);
        } catch (IOException e) {
            LOGGER.error("Unable to serve index.html", e);
            sendResponse(exchange, 500, "Internal Server Error".getBytes());

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
