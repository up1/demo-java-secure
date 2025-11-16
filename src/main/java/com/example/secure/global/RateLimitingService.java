package com.example.secure.global;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {
    // Stores the rate limit bucket for each user ID
    private final Map<String, RateLimitBucket> userBuckets = new ConcurrentHashMap<>();

    // Configuration for the rate limit (5 requests per 60 seconds)
    private static final int MAX_REQUESTS = 5;
    private static final Duration REFILL_DURATION = Duration.ofSeconds(60);

    /**
     * Checks if the authenticated user has available tokens for a request.
     * @param userId The ID of the user.
     * @return true if the request is allowed, false otherwise (rate limit hit).
     */
    public boolean allowRequest(String userId) {
        // Get or create the bucket for the user
        RateLimitBucket bucket = userBuckets.computeIfAbsent(userId,
                k -> new RateLimitBucket(MAX_REQUESTS, MAX_REQUESTS));

        // --- Refill Logic (Token Bucket Algorithm) ---
        Instant now = Instant.now();
        long elapsedSeconds = Duration.between(bucket.lastRefillTime, now).getSeconds();

        if (elapsedSeconds >= REFILL_DURATION.getSeconds()) {
            // If the refill period has passed, replenish the tokens
            bucket.tokens = bucket.capacity;
            bucket.lastRefillTime = now;
        }

        // --- Consumption Logic ---
        if (bucket.tokens > 0) {
            bucket.tokens--;
            return true;
        } else {
            return false;
        }
    }
}
