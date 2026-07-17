package edu.adarko22.spring;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
public class URLShortenerController {
    private final URLShortenerService urlShortenerService;

    public URLShortenerController(URLShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    record ShortenerRequest(String url) {
    }

    record ShortenerResponse(String url, String shortUrl) {
    }

    @PostMapping("/shorten")
    ResponseEntity<ShortenerResponse> urlShortener(@RequestBody ShortenerRequest request) {
        return urlShortenerService.shortenUrl(request.url)
                .map(shortUrl -> {
                    var shortUrlUri = ServletUriComponentsBuilder
                            .fromCurrentContextPath()
                            .path(shortUrl)
                            .build()
                            .toUri();
                    var response = new ShortenerResponse(request.url, shortUrlUri.toString());
                    return ResponseEntity.created(shortUrlUri).body(response);
                })
                .orElseGet(() -> {
                    var response = new ShortenerResponse(request.url, null);
                    return ResponseEntity.badRequest().body(response);
                });
    }

    // Restrict the path variable to only alphanumeric characters using Regex
    @GetMapping("/{shortUrlHash:[a-zA-Z0-9]+}")
    ResponseEntity<Void> redirect(@PathVariable String shortUrlHash) {
        return urlShortenerService.restoreUrl(shortUrlHash)
                .<ResponseEntity<Void>>map(value -> ResponseEntity.status(HttpStatus.FOUND).location(value).build())
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
