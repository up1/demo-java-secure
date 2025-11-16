package com.example.secure.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class DemoController {

    private final ExternalApiService externalApiService;

    public DemoController(ExternalApiService externalApiService) {
        this.externalApiService = externalApiService;
    }

    /**
     * Allows fetching content from an external, validated URL.
     * @param url The URL provided by the user to fetch.
     * @return The content fetched or an error message.
     */
    @GetMapping("/api/v1/fetch-external")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> fetchExternalContent(@RequestParam String url) {

        // --- SSRF Mitigation (API7) ---
        if (!externalApiService.isUrlSafe(url)) {
            // Log the attempt and return a generic error
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: The provided URL is not safe or is restricted.");
        }
        // ------------------------------

        // --- API10: Unsafe Consumption Mitigation ---
        Optional<TodoResponseDTO> result = externalApiService.fetchAndValidateContent(url);

        if (result.isPresent()) {
            // Success: Return the strictly validated and parsed object string
            return ResponseEntity.ok(result.get().toString());
        } else {
            // Failure: Either the external API failed (timeout/circuit breaker) or the data was malformed (output validation).
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Error: External API failed due to timeout, failure threshold (Circuit Breaker), or invalid data structure (API10).");
        }
    }
}
