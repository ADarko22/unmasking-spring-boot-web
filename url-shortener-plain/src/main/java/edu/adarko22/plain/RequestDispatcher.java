package edu.adarko22.plain;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RequestDispatcher implements HttpHandler {

    record Route(String method, String pathRegex) {
        boolean matches(String requestMethod, String requestPath) {
            return this.method.equalsIgnoreCase(requestMethod) && requestPath.matches(this.pathRegex);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestDispatcher.class);

    // LinkedHashMap preserves insertion order, allowing exact routes to be evaluated before catch-all wildcards.
    private final Map<Route, Consumer<HttpExchange>> requestHandlers = new LinkedHashMap<>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var requestMethod = exchange.getRequestMethod();
        var requestPath = exchange.getRequestURI().getPath();

        // Find the first handler that matches the HTTP Method and the Regex path
        var handler = requestHandlers.entrySet()
                .stream()
                .filter(entry -> entry.getKey().matches(requestMethod, requestPath))
                .map(Map.Entry::getValue)
                .findFirst();

        if (handler.isEmpty()) {
            var errorMessage = "No Request Handler found for route %s %s".formatted(requestMethod, requestPath);
            LOGGER.warn(errorMessage);
            sendNotFound(exchange);
            return;
        }

        handler.get().accept(exchange);
    }

    RequestDispatcher addHandler(Route route, Consumer<HttpExchange> handler) {
        if (requestHandlers.containsKey(route)) {
            var errorMessage = "Duplicate Request Handler for route %s".formatted(route);
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        requestHandlers.put(route, handler);

        return this;
    }

    private void sendNotFound(HttpExchange exchange) throws IOException {
        var payload = "404 Not Found".getBytes();
        exchange.sendResponseHeaders(404, payload.length);
        try (var os = exchange.getResponseBody()) {
            os.write(payload);
        }
    }
}
