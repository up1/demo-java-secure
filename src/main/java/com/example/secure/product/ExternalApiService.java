package com.example.secure.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ExternalApiService {

    // --- API10: Circuit Breaker State ---
    private static final int FAILURE_THRESHOLD = 3;
    private static final Duration OPEN_DURATION = Duration.ofSeconds(30);
    // Allow-list for trusted external domains (API7: SSRF Defense)
    private static final List<String> ALLOWED_HOSTS = Arrays.asList("jsonplaceholder.typicode.com", "external-api.trusted.com");
    // Timeout Configuration (API10: Resilience)
    private static final Duration API_TIMEOUT = Duration.ofSeconds(5);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    // ------------------------------------
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private Instant circuitOpenTime = Instant.MIN;

    public ExternalApiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // API10: Configure HTTP client with a connection timeout
        this.httpClient = HttpClient.newBuilder().connectTimeout(API_TIMEOUT).build();
    }

    /**
     * Checks if the authenticated user has available tokens for a request.
     * (SSRF Check - API7)
     */
    public boolean isUrlSafe(String urlString) {
        try {
            URI uri = new URI(urlString);
            String protocol = uri.getScheme();
            String host = uri.getHost();

            // 1. Protocol Validation: Only allow HTTP(s)
            if (protocol == null || !protocol.toLowerCase().matches("https?")) {
                return false;
            }

            // 2. Allow-list Validation: Must be on the allowed host list
            if (host == null || !ALLOWED_HOSTS.contains(host.toLowerCase())) {
                return false;
            }

            // 3. Private IP Check
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isAnyLocalAddress()) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fetches and validates content from an external URL. (API10)
     */
    public Optional<TodoResponseDTO> fetchAndValidateContent(String urlString) {
        // --- API10: Circuit Breaker Check (Resilience) ---
        if (isCircuitOpen()) {
            return Optional.empty();
        }

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlString)).timeout(API_TIMEOUT) // API10: git a Timeout
                .GET().build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                handleFailure(); // API10: Treat non-200 as failure for circuit breaker
                return Optional.empty();
            }

            // --- API10: Strict Output Validation (Sanitization/Deserialization) ---
            // Throws exception if JSON fields are missing or wrong type (e.g., if 'id' is a string instead of an int)
            TodoResponseDTO result = objectMapper.readValue(response.body(), TodoResponseDTO.class);

            // Success: Reset circuit breaker and return validated DTO
            resetCircuit();
            return Optional.of(result);

        } catch (IOException | InterruptedException e) {
            // Network error, connection timeout, or interrupted thread
            handleFailure(); // API10: Treat network/timeout errors as failure
            System.err.println("External API call failed: " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            // JSON parsing/Validation error (API10: Data structure validation failure)
            System.err.println("External API response validation failed: " + e.getMessage());
            resetCircuit(); // The external API might be fine, but the data is bad. Don't trip breaker on bad data.
            return Optional.empty();
        }
    }

    // --- Circuit Breaker Logic ---
    private boolean isCircuitOpen() {
        if (failureCount.get() >= FAILURE_THRESHOLD) {
            // Circuit is open. Check if it's time to half-open/reset.
            if (Instant.now().isAfter(circuitOpenTime.plus(OPEN_DURATION))) {
                // Time's up, allow one request (Half-Open state)
                System.out.println("Circuit is Half-Open: Allowing a probe request.");
                resetCircuit();
                return false; // Allow the request
            }
            System.out.println("Circuit is Open: Blocking request.");
            return true; // Block the request
        }
        return false;
    }

    private void handleFailure() {
        int failures = failureCount.incrementAndGet();
        if (failures >= FAILURE_THRESHOLD) {
            circuitOpenTime = Instant.now();
            System.err.println("Circuit Tripped: Open for " + OPEN_DURATION.getSeconds() + " seconds.");
        }
    }

    private void resetCircuit() {
        failureCount.set(0);
        circuitOpenTime = Instant.MIN;
        System.out.println("Circuit Closed: Failure count reset.");
    }
}