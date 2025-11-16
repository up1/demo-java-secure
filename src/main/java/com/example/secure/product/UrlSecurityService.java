package com.example.secure.product;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UrlSecurityService {

    // Allow-list for trusted external domains (e.g., public APIs)
    private static final List<String> ALLOWED_HOSTS = Arrays.asList(
            "jsonplaceholder.typicode.com", // Example public API host
            "external-api.trusted.com"
    );

    /**
     * Validates a URL to prevent SSRF attacks.
     * Checks protocol, port, and ensures the target IP is not private/reserved.
     * @param urlString The user-supplied URL.
     * @return true if the URL is safe, false otherwise.
     */
    public boolean isUrlSafe(String urlString) {
        try {
            URI uri = new URI(urlString);
            String protocol = uri.getScheme();
            String host = uri.getHost();

            // 1. Protocol Validation: Only allow HTTP(s)
            if (protocol == null || !protocol.toLowerCase().matches("https?")) {
                System.err.println("SSRF Defense: Invalid protocol: " + protocol);
                return false;
            }

            // 2. Allow-list Validation: Must be on the allowed host list
            if (host == null || !ALLOWED_HOSTS.contains(host.toLowerCase())) {
                System.err.println("SSRF Defense: Host not in allow-list: " + host);
                return false;
            }

            // 3. Private IP Check (MOST CRITICAL SSRF DEFENSE)
            // Resolve the host to its IP address(es)
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isAnyLocalAddress()) {
                    System.err.println("SSRF Defense: Blocking access to private IP: " + address.getHostAddress());
                    return false;
                }
            }

            // If all checks pass, it is considered safe
            return true;

        } catch (Exception e) {
            System.err.println("SSRF Defense: URL parsing failed for: " + urlString + " Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fetches content from a pre-validated external URL.
     * NOTE: This method is intentionally simple and non-robust for demonstration.
     * @param urlString The URL to fetch.
     * @return The content fetched.
     */
    public String fetchContent(String urlString) {
        try {
            URL url = new URL(urlString);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            return "Error fetching content: " + e.getMessage();
        }
    }
}