package edu.adarko22.spring;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@Service
public class URLShortenerService {

    private final ConcurrentHashMap<String, String> urlToHash = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> hashToUrl = new ConcurrentHashMap<>();
    private final HashGenerator hashGenerator = new HashGenerator();

    Optional<String> shortenUrl(String url) {
        if (!isValidUrl(url))
            return Optional.empty();

        // the compute function is run atomically
        var shortHash = urlToHash.computeIfAbsent(url, unused -> {
            var newHash = computeUniqueHash();
            hashToUrl.put(newHash, url);
            return newHash;
        });

        return Optional.of(shortHash);
    }

    Optional<URI> restoreUrl(String shortHash) {
        return Optional.ofNullable(hashToUrl.get(shortHash)).map(URI::create);
    }

    private boolean isValidUrl(String url) {
        try {
            new URI(url);
        } catch (URISyntaxException e) {
            return false;
        }
        return true;
    }

    private String computeUniqueHash() {
        var shortUrl = hashGenerator.hash();

        while (hashToUrl.containsKey(shortUrl)) {
            shortUrl = hashGenerator.hash();
        }

        return shortUrl;
    }

    static class HashGenerator {
        private static final int shaLen = 5;
        private static final String shaDictionary = "1234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";
        private static final Random random = new Random();

        String hash() {
            return IntStream.range(0, shaLen)
                    .map(unused -> random.nextInt(shaDictionary.length()))
                    .mapToObj(shaDictionary::charAt)
                    .map(String::valueOf)
                    .reduce("", String::concat);
        }
    }
}
