package com.example.secure.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    private final UrlSecurityService urlSecurityService;

    public DemoController(UrlSecurityService urlSecurityService) {
        this.urlSecurityService = urlSecurityService;
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
        if (!urlSecurityService.isUrlSafe(url)) {
            // Log the attempt and return a generic error
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: The provided URL is not safe or is restricted.");
        }
        // ------------------------------

        String content = urlSecurityService.fetchContent(url);

        return ResponseEntity.ok(content);
    }
}
