package edu.adarko22.plain;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class URLShortenerControllerTest {

    private static HttpServer server;
    private static String serverBaseUrl;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final URLShortenerController controller = new URLShortenerController();

    @BeforeAll
    static void startServer() throws IOException {
        // Port 0 => random available system port
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/shorten", controller::urlShortener);

        // Handling dynamic catch-all route mapping for redirects at root
        server.createContext("/", exchange -> {
            // Filter out /shorten requests so they don't hit the redirect engine
            if (exchange.getRequestURI().getPath().startsWith("/shorten")) {
                controller.urlShortener(exchange);
            } else {
                controller.redirect(exchange);
            }
        });

        // default executor
        server.setExecutor(null);
        server.start();

        // Dynamically capture the runtime host address
        serverBaseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testShortenAndRedirectSuccess() throws Exception {
        var originalUrl = "https://www.example.com";
        var requestDto = new URLShortenerController.ShortenerRequest(originalUrl);
        var jsonRequest = objectMapper.writeValueAsString(requestDto);

        // 1. Execute POST Shorten Request
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + "/shorten"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, postResponse.statusCode());

        var responseBody = objectMapper.readValue(postResponse.body(), URLShortenerController.ShortenerResponse.class);
        assertNotNull(responseBody.shortUrl());
        assertEquals(originalUrl, responseBody.url());

        // 2. Execute GET Redirect Request
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(responseBody.shortUrl()))
                .GET()
                .build();

        // Note: Java HttpClient follows redirects by default unless configured.
        HttpClient redirectionClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        HttpResponse<Void> getResponse = redirectionClient.send(getRequest, HttpResponse.BodyHandlers.discarding());
        assertEquals(302, getResponse.statusCode());
        assertEquals(originalUrl, getResponse.headers().firstValue("Location").orElse(null));
    }

    @Test
    void testShortenFailure() throws Exception {
        var badOriginalUrl = "bad-uri..format .com";
        var requestDto = new URLShortenerController.ShortenerRequest(badOriginalUrl);
        var jsonRequest = objectMapper.writeValueAsString(requestDto);

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + "/shorten"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, postResponse.statusCode());
    }

    @Test
    void testRedirectFailure() throws Exception {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(serverBaseUrl + "/non-existing-hash"))
                .GET()
                .build();

        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, getResponse.statusCode());
    }
}
