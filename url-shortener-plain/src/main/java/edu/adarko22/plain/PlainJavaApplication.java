package edu.adarko22.plain;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class PlainJavaApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlainJavaApplication.class);

    public static void main(String[] args) throws IOException {
        var port = 3001;

        var requestDispatcher = createRequestHandlers();

        var server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", requestDispatcher);

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        var logMessage = "Plain Java Server started on port %s...".formatted(port);
        LOGGER.info(logMessage);

        server.start();
    }

    private static RequestDispatcher createRequestHandlers() {
        var urlShortenerController = new URLShortenerController();
        return new RequestDispatcher()
                .addHandler(
                        new RequestDispatcher.Route("POST", "^/shorten$"),
                        urlShortenerController::urlShortener
                )
                .addHandler(
                        new RequestDispatcher.Route("GET", "^/[a-zA-Z0-9]+$"),
                        urlShortenerController::redirect
                );
    }
}