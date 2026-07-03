package edu.adarko22.spring;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
                    var response = new ShortenerResponse(request.url, shortUrl);
                    var shortUrlUri = URI.create(shortUrl);
                    return ResponseEntity.created(shortUrlUri).body(response);
                })
                .orElseGet(() -> {
                    var response = new ShortenerResponse(request.url, null);
                    return ResponseEntity.badRequest().body(response);
                });
    }

    @GetMapping("/{shortUrlHash}")
    ResponseEntity<Void> redirect(@PathVariable String shortUrlHash) {
        return urlShortenerService.restoreUrl(shortUrlHash)
                .<ResponseEntity<Void>>map(value -> ResponseEntity.status(HttpStatus.FOUND).location(value).build())
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
